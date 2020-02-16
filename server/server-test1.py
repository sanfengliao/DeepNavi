import argparse
import errno
import json
import logging
import os
import struct
import sys
import threading
import time
from abc import ABCMeta, abstractmethod
from base64 import b64encode
from hashlib import sha1
from socket import AF_INET, SOCK_DGRAM
from socket import error as SocketError
from socket import socket
from socketserver import StreamRequestHandler, TCPServer, ThreadingMixIn
from typing import Tuple, Union

path = os.path.dirname(os.path.abspath(__file__))
sys.path.append(path)

from proto_model.basic_pb2 import DeepNaviReq, DeepNaviRes

FIN = 0x80
OP_CODE = 0x0f
MASKED = 0x80
PAYLOAD_LEN = 0x7f
PAYLOAD_LEN_EXT16 = 0x7e
PAYLOAD_LEN_EXT64 = 0x7f

OP_CODE_CONTINUATION = 0x0
OP_CODE_TEXT = 0x1
OP_CODE_BINARY = 0x2
OP_CODE_CLOSE_CONN = 0x8
OP_CODE_PING = 0x9
OP_CODE_PONG = 0xA

# closing frame status codes.
STATUS_NORMAL = 1000
STATUS_GOING_AWAY = 1001
STATUS_PROTOCOL_ERROR = 1002
STATUS_UNSUPPORTED_DATA_TYPE = 1003
STATUS_STATUS_NOT_AVAILABLE = 1005
STATUS_ABNORMAL_CLOSED = 1006
STATUS_INVALID_PAYLOAD = 1007
STATUS_POLICY_VIOLATION = 1008
STATUS_MESSAGE_TOO_BIG = 1009
STATUS_INVALID_EXTENSION = 1010
STATUS_UNEXPECTED_CONDITION = 1011
STATUS_BAD_GATEWAY = 1014
STATUS_TLS_HANDSHAKE_ERROR = 1015

VALID_CLOSE_STATUS = (
    STATUS_NORMAL,
    STATUS_GOING_AWAY,
    STATUS_PROTOCOL_ERROR,
    STATUS_UNSUPPORTED_DATA_TYPE,
    STATUS_INVALID_PAYLOAD,
    STATUS_POLICY_VIOLATION,
    STATUS_MESSAGE_TOO_BIG,
    STATUS_INVALID_EXTENSION,
    STATUS_UNEXPECTED_CONDITION,
    STATUS_BAD_GATEWAY,
)

SHOW_BYTE_LEN_LIMIT = 128


def define_log(log_dir_path: str, max_retain: int):
    log_dir_path = os.path.abspath(log_dir_path)
    if not os.path.exists(log_dir_path) or not os.path.isdir(log_dir_path):
        print('log path not exists and now create it: {}.'.format(log_dir_path))
        os.mkdir(log_dir_path)
    log_file_name = '{}.log'.format(time.strftime(
        "%Y-%m-%d %H-%M-%S", time.localtime()))
    log_file_path = os.path.abspath(os.path.join(log_dir_path, log_file_name))

    log_file_list = sorted(os.listdir(log_dir_path), reverse=True)
    for out_date_log_file_name in log_file_list[max_retain - 1:]:
        out_date_log_file_path = os.path.abspath(
            os.path.join(log_dir_path, out_date_log_file_name))
        print('too many log file, now delete one: {}'.format(
            out_date_log_file_path))
        os.remove(out_date_log_file_path)

    log_formatter = logging.Formatter(
        "%(asctime)s - %(name)s - [%(threadName)-12.12s] - [%(levelname)-5.5s] -  %(message)s")
    root_logger = logging.getLogger()
    root_logger.handlers = []  # 似乎原有的就有 console_handler 了
    file_handler = logging.FileHandler(log_file_path)
    file_handler.setFormatter(log_formatter)
    root_logger.addHandler(file_handler)
    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setFormatter(log_formatter)
    root_logger.addHandler(console_handler)
    root_logger.setLevel(logging.NOTSET)


class WebsocketServer(ThreadingMixIn, TCPServer):
    TAG = "WebsocketServer"

    allow_reuse_address = True
    daemon_threads = True

    def __init__(self, port: int, host: str = '127.0.0.1'):
        TCPServer.__init__(self, (host, port), WebSocketHandler)
        self.port = self.socket.getsockname()[1]
        logging.debug("{}.__init__(port: {}, host: {})".format(
            self.TAG, self.port, host))
        self.new_client = None
        self.client_left = None
        self.bytes_received = None
        self.message_received = None
        self.ping_received = None
        self.clients = dict()
        self.id_counter = -1

    def run_forever(self):
        try:
            logging.debug(
                "{}.run_forever - Listening on port {} for clients..".format(self.TAG, self.port))
            self.serve_forever()
        except KeyboardInterrupt:
            self.server_close()
            logging.debug(
                "{}.run_forever - Server terminated.".format(self.TAG))
        except Exception as e:
            logging.error("{}.run_forever - Server exception: {}".format(
                self.TAG, e), exc_info=True)
            exit(1)

    def _message_received_(self, handler, msg: str):
        len_msg = len(msg)
        flag = len_msg > SHOW_BYTE_LEN_LIMIT
        logging.debug(
            "{}._message_received_(clientId: {}, msg: {})".format(self.TAG, handler.id, len_msg if flag else msg))
        if self.message_received is None:
            logging.debug(
                "{}._message_received_(clientId: {}, msg: {}) -- message_received is None".format(
                    self.TAG, handler.id, len_msg if flag else msg))
        elif handler.id not in self.clients.keys():
            logging.debug(
                "{}._message_received_(clientId: {}, msg: {}) -- client has been removed".format(
                    self.TAG, handler.id, len_msg if flag else msg))
        else:
            self.message_received(self.clients[handler.id], self, msg)

    def _bytes_received_(self, handler, msg: bytes):
        len_msg = len(msg)
        flag = len_msg > SHOW_BYTE_LEN_LIMIT
        logging.debug(
            "{}._bytes_received_(clientId: {}, msg: {})".format(self.TAG, handler.id, len_msg if flag else msg))
        if self.bytes_received is None:
            logging.debug(
                "{}._bytes_received_(clientId: {}, msg: {}) -- bytes_received is None".format(
                    self.TAG, handler.id, len_msg if flag else msg))
        elif handler.id not in self.clients.keys():
            logging.debug(
                "{}._bytes_received_(clientId: {}, msg: {}) -- client has been removed".format(
                    self.TAG, handler.id, len_msg if flag else msg))
        else:
            self.bytes_received(self.clients[handler.id], self, msg)

    def _ping_received_(self, handler, msg: str):
        len_msg = len(msg)
        flag = len_msg > SHOW_BYTE_LEN_LIMIT
        logging.debug("{}._ping_received_(clientId: {}, msg: {})".format(
            self.TAG, handler.id, len_msg if flag else msg))
        handler.send_pong(msg)
        if self.ping_received is None:
            logging.debug(
                "{}._ping_received_(clientId: {}, msg: {}) -- ping_received is None".format(
                    self.TAG, handler.id, len_msg if flag else msg))
        elif handler.id not in self.clients.keys():
            logging.debug(
                "{}._ping_received_(clientId: {}, msg: {}) -- client has been removed".format(
                    self.TAG, handler.id, len_msg if flag else msg))
        else:
            self.ping_received(self.clients[handler.id], self, msg)

    def _pong_received_(self, handler, msg: str):
        len_msg = len(msg)
        flag = len_msg > SHOW_BYTE_LEN_LIMIT
        logging.debug("{}._pong_received_(clientId: {}, msg: {})".format(
            self.TAG, handler.id, len_msg if flag else msg))

    def _new_client_(self, handler):
        self.id_counter += 1
        logging.debug("{}._new_client_(clientId: {}, clientAddress: {})".format(
            self.TAG, self.id_counter, handler.client_address))
        client = {
            'id': self.id_counter,
            'handler': handler,
            'address': handler.client_address
        }
        handler.id = self.id_counter
        self.clients[self.id_counter] = client
        if self.new_client is not None:
            self.new_client(client, self)
        else:
            logging.debug(
                '{}._new_client_(clientId: {}, clientAddress: {}) -- new_client is None'.format(
                    self.TAG, self.id_counter, handler.client_address))

    def _client_left_(self, handler):
        logging.debug("{}._client_left_(clientId: {}, clientAddress: {})".format(
            self.TAG, handler.id, handler.client_address))
        if handler.id in self.clients.keys():
            client = self.clients[handler.id]
        else:
            logging.debug(
                "{}._client_left_(clientId: {}, clientAddress: {}) -- client has been removed".format(
                    self.TAG, handler.id, handler.client_address))
            return
        if self.client_left is not None:
            self.client_left(client, self)
        else:
            logging.debug(
                '{}._client_left_(clientId: {}, clientAddress: {}) -- client_left is None'.format(
                    self.TAG, client['id'], handler.client_address))
        if handler.id in self.clients.keys():
            self.clients.pop(handler.id)

    def send_message(self, client: dict, msg: str):
        len_msg = len(msg)
        flag = len_msg > SHOW_BYTE_LEN_LIMIT
        logging.debug("{}.send_message(clientId: {}, clientAddress: {}, msg: {})".format(
            self.TAG, client['id'], client['handler'].client_address, len_msg if flag else msg))
        client['handler'].send_message(msg)

    def send_bytes(self, client: dict, msg: bytes):
        len_msg = len(msg)
        flag = len_msg > SHOW_BYTE_LEN_LIMIT
        logging.debug("{}.send_bytes(clientId: {}, clientAddress: {}, msg: {})".format(
            self.TAG, client['id'], client['handler'].client_address, len_msg if flag else try_decode_utf8(msg)))
        client['handler'].send_text(msg, op_code=OP_CODE_BINARY)

    def send_message_to_all(self, msg: str):
        len_msg = len(msg)
        flag = len_msg > SHOW_BYTE_LEN_LIMIT
        logging.debug("{}.send_message(msg: {})".format(
            self.TAG, len_msg if flag else msg))
        for client in self.clients:
            self.send_message(client, msg)

    def send_close(self, client: dict, status: int = STATUS_NORMAL, reason: str = "normal close"):
        client_id = client['id']
        client_handler = client['handler']
        logging.debug(
            "{}.send_close(clientId: {}, clientAddress: {}, reason: {}, status: {})".format(
                self.TAG, client_id, client_handler, reason, status))
        client_handler.send_close(status, reason)
        # self._client_left_(client_handler)


class WebSocketHandler(StreamRequestHandler):
    TAG = "WebSocketHandler"

    def __init__(self, sock: socket, addr: str, server: WebsocketServer):
        self.server = server
        self.keep_alive = True
        self.handshake_done = False
        self.valid_client = False
        self.sock = sock
        self.id = -1
        StreamRequestHandler.__init__(self, sock, addr, server)

    def handle(self):
        logging.debug("{} -- begin handle".format(self.TAG))
        while self.keep_alive:
            if not self.handshake_done:
                self.handshake()
            elif self.valid_client:
                self.read_next_message()
        logging.debug("{} -- finish handle".format(self.TAG))

    def read_bytes(self, num: int) -> bytes:
        result_bytes = self.rfile.read(num)
        len_bytes = len(result_bytes)
        flag = len_bytes > SHOW_BYTE_LEN_LIMIT
        logging.debug("{} -- read_bytes({}: {})".format(
            self.TAG, 'len_bytes' if flag else 'result_bytes', num if flag else result_bytes))
        return result_bytes

    def read_next_message(self):
        try:
            b1, b2 = self.read_bytes(2)
        except SocketError as ex:  # to be replaced with ConnectionResetError for py3
            if ex.errno == errno.ECONNRESET:
                logging.debug(
                    "{}.read_next_message -- Client closed connection.".format(self.TAG))
                self.keep_alive = False
                return
            logging.debug(
                "{}.read_next_message -- socket error: {}".format(self.TAG, ex))
            b1, b2 = 0, 0
        except ValueError as ex:
            logging.debug(
                "{}.read_next_message -- other error: {}".format(self.TAG, ex))
            b1, b2 = 0, 0

        fin = b1 & FIN
        op_code = b1 & OP_CODE
        masked = b2 & MASKED
        payload_length = b2 & PAYLOAD_LEN
        logging.debug(
            "{}.read_next_message -- fin: {}, op_code: {}, masked: {}, payload_length: {}".format(
                self.TAG, fin, op_code, masked, payload_length))

        if op_code == OP_CODE_CLOSE_CONN:
            logging.debug(
                "{}.read_next_message -- Client asked to close connection.".format(self.TAG))
            self.keep_alive = False
            return
        if not masked:
            logging.warning(
                "{}.read_next_message -- Client must always be masked.".format(self.TAG))
            self.keep_alive = False
            return
        if op_code == OP_CODE_CONTINUATION:
            logging.warning(
                "{}.read_next_message -- Continuation frames are not supported.".format(self.TAG))
            return
        if op_code == OP_CODE_BINARY:
            op_code_handler = self.server._bytes_received_
        elif op_code == OP_CODE_TEXT:
            op_code_handler = self.server._message_received_
        elif op_code == OP_CODE_PING:
            op_code_handler = self.server._ping_received_
        elif op_code == OP_CODE_PONG:
            op_code_handler = self.server._pong_received_
        else:
            logging.warning(
                "%s.read_next_message -- Unknown op_code %#x." % (self.TAG, op_code))
            self.keep_alive = False
            return

        if payload_length == 126:
            payload_length = struct.unpack(">H", self.rfile.read(2))[0]
        elif payload_length == 127:
            payload_length = struct.unpack(">Q", self.rfile.read(8))[0]

        masks = self.read_bytes(4)
        message_bytes = bytearray()
        for message_byte in self.read_bytes(payload_length):
            message_byte ^= masks[len(message_bytes) % 4]
            message_bytes.append(message_byte)
        logging.debug(
            "{}.read_next_message -- message_bytes.len: {}".format(self.TAG, len(message_bytes)))
        if op_code == OP_CODE_BINARY:
            op_code_handler(self, message_bytes)
        else:
            op_code_handler(self, message_bytes.decode('utf8'))

    def send_message(self, message: str):
        self.send_text(message)

    def send_pong(self, message: str):
        self.send_text(message, OP_CODE_PONG)

    def send_close(self, status: int = STATUS_NORMAL, reason: str = "normal close"):
        if status < 0 or status >= 1 << 16:
            raise ValueError("code is invalid range")
        if self.keep_alive:
            self.keep_alive = False
            self.send_text(struct.pack('!H', status) +
                           bytes(reason, 'utf8'), OP_CODE_CLOSE_CONN)

    def send_text(self, message: Union[str, bytes], op_code: int = OP_CODE_TEXT):
        if not isinstance(message, (bytes, str)):
            logging.warning(
                '{}.send_text -- Can\'t send message, message has to be a string or bytes. Given type is {}'
                .format(self.TAG, type(message)))
            return
        header = bytearray()
        payload = encode_to_utf8(message) if isinstance(
            message, str) else message
        payload_length = len(payload)
        header.append(FIN | op_code)

        # Normal payload
        if payload_length <= 125:
            header.append(payload_length)
        # Extended payload
        elif 126 <= payload_length <= 65535:
            header.append(PAYLOAD_LEN_EXT16)
            header.extend(struct.pack(">H", payload_length))
        # Huge extended payload
        elif payload_length < 18446744073709551616:
            header.append(PAYLOAD_LEN_EXT64)
            header.extend(struct.pack(">Q", payload_length))
        else:
            ex = Exception(
                "Message is too big. Consider breaking it into chunks.")
            logging.warning(
                '{}.send_text -- payload exception: {}'.format(self.TAG, ex))
            raise ex

        self.request.send(header + payload)

    def read_http_headers(self) -> dict:
        headers = {}
        # first line should be HTTP GET
        http_get = self.rfile.readline().decode().strip()
        assert http_get.upper().startswith('GET')
        # remaining should be headers
        while True:
            header = self.rfile.readline().decode().strip()
            if not header:
                break
            head, value = header.split(':', 1)
            headers[head.lower().strip()] = value.strip()
        logging.debug(
            "{}.read_http_headers -- headers: {}".format(self.TAG, headers))
        return headers

    def handshake(self):
        logging.debug("{}.handshake".format(self.TAG))
        headers = self.read_http_headers()

        try:
            assert headers['upgrade'].lower() == 'websocket'
        except AssertionError as ex:
            self.keep_alive = False
            logging.warning(
                '{}.handshake -- AssertionError: {}'.format(self.TAG, ex))
            return

        try:
            key = headers['sec-websocket-key']
        except KeyError as ex:
            logging.warning(
                "{}.handshake -- Client tried to connect but was missing a key -- {}".format(
                    self.TAG, ex))
            self.keep_alive = False
            return

        response = self.make_handshake_response(key)
        self.handshake_done = self.request.send(response.encode())
        self.valid_client = True
        self.server._new_client_(self)

    @classmethod
    def make_handshake_response(cls, key: str) -> str:
        logging.debug(
            "{}.make_handshake_response(key: {})".format(cls.TAG, key))
        return \
            'HTTP/1.1 101 Switching Protocols\r\n' \
            'Upgrade: websocket\r\n' \
            'Connection: Upgrade\r\n' \
            'Sec-WebSocket-Accept: %s\r\n' \
            '\r\n' % cls.calculate_response_key(key)

    @classmethod
    def calculate_response_key(cls, key: str) -> bytes:
        guid = '258EAFA5-E914-47DA-95CA-C5AB0DC85B11'
        hash_value = sha1(key.encode() + guid.encode())
        response_key = b64encode(hash_value.digest()).strip().decode('ASCII')
        logging.debug("{}.calculate_response_key(key: {}) -- response_key: {}".format(
            cls.TAG, key, response_key))
        return response_key

    def finish(self):
        logging.debug("{}.finish".format(self.TAG))
        self.server._client_left_(self)


def encode_to_utf8(data: str) -> Union[bytes, bool]:
    len_data = len(data)
    flag = len_data > SHOW_BYTE_LEN_LIMIT
    try:
        return data.encode('UTF-8')
    except UnicodeEncodeError as e:
        logging.error(
            "unicode - Could not encode data(msg: {}) to UTF-8 -- {}".format(len_data if flag else data, e))
        return False
    except Exception as e:
        logging.error(
            "unicode - Other exception while encode data(msg: {}) -- {}".format(len_data if flag else data, e))
        raise e


def try_decode_utf8(data: bytes) -> Union[str, bool]:
    len_data = len(data)
    flag = len_data > SHOW_BYTE_LEN_LIMIT
    try:
        return data.decode('utf-8')
    except UnicodeDecodeError as e:
        logging.error(
            "unicode - Could not encode data(msg: {}) to UTF-8 -- {}".format(len_data if flag else data, e))
        return False
    except Exception as e:
        logging.error(
            "unicode - Other exception while decode data(msg: {}) -- {}".format(len_data if flag else data, e))
        raise e


class RepeatingTimer(threading.Timer):

    def __init__(self, interval: int, function, log_interval: int = 1):
        self.finished = None
        self.interval = interval
        self.function = function
        self.args = None
        self.kwargs = None
        super(RepeatingTimer, self).__init__(interval, function)
        self.log_interval = log_interval
        self.log_counter = 0

    def run(self):
        temp = self.log_interval * self.interval
        while not self.finished.is_set():
            self.finished.wait(self.interval)
            self.function(*self.args, **self.kwargs)
            self.log_counter += 1
            if self.log_counter >= self.log_interval:
                logging.debug('RepeatingTimer: run -- wait {}s and log {}s -- execute {}'.format(
                    self.interval, temp, self.function))
                self.log_counter = 0


class UserIdentityHandler(metaclass=ABCMeta):
    # TODO:

    @abstractmethod
    def addUser(self, client: dict):
        pass

    @abstractmethod
    def removeUser(self, client: dict):
        pass

    @abstractmethod
    def getUser(self, client: dict):
        pass


class DeepNaviServer:
    TAG = "DeepNaviServer"
    IMAGE_PATH = os.path.abspath("./images/")

    # 注意: client的这个dict的组成是 {'id': int, 'handler': WebSocketHandler, 'address': (addr, port)}
    # 然后需要发消息的时候，只要
    #     * self.websocket_server.send_message(client: dict, message: string)
    #     * self.websocket_server.send_bytes(client: dict, message: bytes)
    #     * self.websocket_server.send_close(clent: dict)

    def __init__(self, port: int, host: str):
        # websocket server
        self.websocket_server = WebsocketServer(port, host)
        self.websocket_server.new_client = self.new_client
        self.websocket_server.client_left = self.client_left
        self.websocket_server.message_received = self.message_received
        self.websocket_server.ping_received = self.ping_received
        self.websocket_server.bytes_received = self.bytes_received
        # clients

    def new_client(self, client: dict, server: WebsocketServer):
        # TODO:
        logging.debug('{}.{} -- client: (id: {}, address: {})'.format(
            self.TAG, 'new_client', client['id'], client['address']))
        if not os.path.exists(self.IMAGE_PATH) or not os.path.isdir(self.IMAGE_PATH):
            logging.debug(
                '{}.{} -- create directory for images: {}'.format(self.TAG, 'new_client', self.IMAGE_PATH))
            os.mkdir(self.IMAGE_PATH)
        # client_self.image_path =
        # if not os.path.exists(self.IMAGE_PATH) or not os.path.isdir(self.IMAGE_PATH):
        #     logging.debug(
        #         '{}.{} -- create directory for images: {}'.format(self.TAG, 'new_client', self.IMAGE_PATH))
        #     os.mkdir(self.IMAGE_PATH)
        pass

    def client_left(self, client: dict, server: WebsocketServer):
        # TODO:
        logging.debug('{}.{} -- client: (id: {}, address: {})'.format(
            self.TAG, 'client_left', client['id'], client['address']))
        pass

    def message_received(self, client: dict, server: WebsocketServer, message: str):
        # TODO:
        len_msg = len(message)
        flag = len_msg > 128
        logging.debug('{}.{} -- client: (id: {}, address: {}), {}: {}'.format(
            self.TAG, 'message_received', client['id'], client['address'], 'len_msg' if flag else 'message', len_msg if flag else message))

        # deepNaviReq = DeepNaviReq()
        # deepNaviReq.ParseFromString(message)
        # f = open(self.IMAGE_PATH + '/' + str(deepNaviReq.time) + '.jpg', 'wb+')
        # f.write(deepNaviReq.image)
        # f.close()

        # deepNaviRes = DeepNaviRes()
        # deepNaviRes.result = 'OK'
        # server.send_message(client, deepNaviRes.SerializeToString())

    def bytes_received(self, client: dict, server: WebsocketServer, message: bytes):
        # TODO:
        len_msg = len(message)
        flag = len_msg > 128
        logging.debug('{}.{} -- client: (id: {}, address: {}), {}: {}'.format(
            self.TAG, 'bytes_received', client['id'], client['address'], 'len_msg' if flag else 'message', len_msg if flag else message))

        deepNaviReq = DeepNaviReq()
        deepNaviReq.ParseFromString(message)
        f = open(self.IMAGE_PATH + '/' + str(deepNaviReq.time) + '.jpg', 'wb+')
        f.write(deepNaviReq.image)
        f.close()

        deepNaviRes = DeepNaviRes()
        deepNaviRes.result = 'OK'
        server.send_bytes(client, deepNaviRes.SerializeToString())

    def ping_received(self, client: dict, server: WebsocketServer, message: str):
        # TODO:
        logging.debug('{}.{} -- client: (id: {}, address: {})'.format(
            self.TAG, 'ping_received', client['id'], client['address']))
        pass

    def start(self):
        self.websocket_server.run_forever()


def get_host_ip():
    ip = None
    try:
        s = socket(AF_INET, SOCK_DGRAM)
        s.connect(('8.8.8.8', 80))
        ip = s.getsockname()[0]
    finally:
        s.close()
    return ip


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description="WebSocket Simple Server Tool", add_help=True)
    parser.add_argument("-p", "--port", type=int, default=9001,
                        help="Port for websocket listening")
    parser.add_argument("-l", "--log_files", type=str, default="./server_logs",
                        help="Folder for log files")
    args = parser.parse_args()
    print('parse_args: port({}), log_files({})'.format(args.port, args.log_files))

    define_log(args.log_files, 10)

    host = get_host_ip()
    if host is None:
        logging.debug("get ip error!!!")
    else:
        logging.debug("host is " + host)
        print("host is " + host)
        DeepNaviServer(args.port, host).start()
