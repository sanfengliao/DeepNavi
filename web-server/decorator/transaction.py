def transaction(func):
    def transactionWrap(*args, **kwargs):
        result = None
        session = client.start_session()
        with session.start_transaction():
            result = func(*args, **kwargs)
        return result
    return transactionWrap
from dao.mongodb import client