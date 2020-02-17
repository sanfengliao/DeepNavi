# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: basic.proto

import sys
_b=sys.version_info[0]<3 and (lambda x:x) or (lambda x:x.encode('latin1'))
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()




DESCRIPTOR = _descriptor.FileDescriptor(
  name='basic.proto',
  package='',
  syntax='proto2',
  serialized_options=_b('\n\026com.sysu.deepnavi.beanB\005Basic'),
  serialized_pb=_b('\n\x0b\x62\x61sic.proto\"\x1e\n\rFeelSensorReq\x12\r\n\x05value\x18\x01 \x02(\x02\"0\n\rCoorSensorReq\x12\t\n\x01x\x18\x01 \x02(\x02\x12\t\n\x01y\x18\x02 \x02(\x02\x12\t\n\x01z\x18\x03 \x02(\x02\"\xc0\x03\n\x0b\x44\x65\x65pNaviReq\x12\x0c\n\x04time\x18\x01 \x02(\x03\x12\r\n\x05image\x18\x02 \x01(\x0c\x12$\n\x0cmagneticList\x18\x03 \x03(\x0b\x32\x0e.CoorSensorReq\x12)\n\x11\x61\x63\x63\x65lerometerList\x18\x04 \x03(\x0b\x32\x0e.CoorSensorReq\x12\'\n\x0forientationList\x18\x05 \x03(\x0b\x32\x0e.CoorSensorReq\x12%\n\rgyroscopeList\x18\x06 \x03(\x0b\x32\x0e.CoorSensorReq\x12#\n\x0bgravityList\x18\x07 \x03(\x0b\x32\x0e.CoorSensorReq\x12.\n\x16linearAccelerationList\x18\x08 \x03(\x0b\x32\x0e.CoorSensorReq\x12.\n\x16\x61mbientTemperatureList\x18\t \x03(\x0b\x32\x0e.FeelSensorReq\x12!\n\tlightList\x18\n \x03(\x0b\x32\x0e.FeelSensorReq\x12$\n\x0cpressureList\x18\x0b \x03(\x0b\x32\x0e.FeelSensorReq\x12%\n\rproximityList\x18\x0c \x03(\x0b\x32\x0e.FeelSensorReq\"\x1d\n\x0b\x44\x65\x65pNaviRes\x12\x0e\n\x06result\x18\x01 \x02(\tB\x1f\n\x16\x63om.sysu.deepnavi.beanB\x05\x42\x61sic')
)




_FEELSENSORREQ = _descriptor.Descriptor(
  name='FeelSensorReq',
  full_name='FeelSensorReq',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='value', full_name='FeelSensorReq.value', index=0,
      number=1, type=2, cpp_type=6, label=2,
      has_default_value=False, default_value=float(0),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto2',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=15,
  serialized_end=45,
)


_COORSENSORREQ = _descriptor.Descriptor(
  name='CoorSensorReq',
  full_name='CoorSensorReq',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='x', full_name='CoorSensorReq.x', index=0,
      number=1, type=2, cpp_type=6, label=2,
      has_default_value=False, default_value=float(0),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='y', full_name='CoorSensorReq.y', index=1,
      number=2, type=2, cpp_type=6, label=2,
      has_default_value=False, default_value=float(0),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='z', full_name='CoorSensorReq.z', index=2,
      number=3, type=2, cpp_type=6, label=2,
      has_default_value=False, default_value=float(0),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto2',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=47,
  serialized_end=95,
)


_DEEPNAVIREQ = _descriptor.Descriptor(
  name='DeepNaviReq',
  full_name='DeepNaviReq',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='time', full_name='DeepNaviReq.time', index=0,
      number=1, type=3, cpp_type=2, label=2,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='image', full_name='DeepNaviReq.image', index=1,
      number=2, type=12, cpp_type=9, label=1,
      has_default_value=False, default_value=_b(""),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='magneticList', full_name='DeepNaviReq.magneticList', index=2,
      number=3, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='accelerometerList', full_name='DeepNaviReq.accelerometerList', index=3,
      number=4, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='orientationList', full_name='DeepNaviReq.orientationList', index=4,
      number=5, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='gyroscopeList', full_name='DeepNaviReq.gyroscopeList', index=5,
      number=6, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='gravityList', full_name='DeepNaviReq.gravityList', index=6,
      number=7, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='linearAccelerationList', full_name='DeepNaviReq.linearAccelerationList', index=7,
      number=8, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='ambientTemperatureList', full_name='DeepNaviReq.ambientTemperatureList', index=8,
      number=9, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='lightList', full_name='DeepNaviReq.lightList', index=9,
      number=10, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='pressureList', full_name='DeepNaviReq.pressureList', index=10,
      number=11, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='proximityList', full_name='DeepNaviReq.proximityList', index=11,
      number=12, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto2',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=98,
  serialized_end=546,
)


_DEEPNAVIRES = _descriptor.Descriptor(
  name='DeepNaviRes',
  full_name='DeepNaviRes',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='result', full_name='DeepNaviRes.result', index=0,
      number=1, type=9, cpp_type=9, label=2,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto2',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=548,
  serialized_end=577,
)

_DEEPNAVIREQ.fields_by_name['magneticList'].message_type = _COORSENSORREQ
_DEEPNAVIREQ.fields_by_name['accelerometerList'].message_type = _COORSENSORREQ
_DEEPNAVIREQ.fields_by_name['orientationList'].message_type = _COORSENSORREQ
_DEEPNAVIREQ.fields_by_name['gyroscopeList'].message_type = _COORSENSORREQ
_DEEPNAVIREQ.fields_by_name['gravityList'].message_type = _COORSENSORREQ
_DEEPNAVIREQ.fields_by_name['linearAccelerationList'].message_type = _COORSENSORREQ
_DEEPNAVIREQ.fields_by_name['ambientTemperatureList'].message_type = _FEELSENSORREQ
_DEEPNAVIREQ.fields_by_name['lightList'].message_type = _FEELSENSORREQ
_DEEPNAVIREQ.fields_by_name['pressureList'].message_type = _FEELSENSORREQ
_DEEPNAVIREQ.fields_by_name['proximityList'].message_type = _FEELSENSORREQ
DESCRIPTOR.message_types_by_name['FeelSensorReq'] = _FEELSENSORREQ
DESCRIPTOR.message_types_by_name['CoorSensorReq'] = _COORSENSORREQ
DESCRIPTOR.message_types_by_name['DeepNaviReq'] = _DEEPNAVIREQ
DESCRIPTOR.message_types_by_name['DeepNaviRes'] = _DEEPNAVIRES
_sym_db.RegisterFileDescriptor(DESCRIPTOR)

FeelSensorReq = _reflection.GeneratedProtocolMessageType('FeelSensorReq', (_message.Message,), dict(
  DESCRIPTOR = _FEELSENSORREQ,
  __module__ = 'basic_pb2'
  # @@protoc_insertion_point(class_scope:FeelSensorReq)
  ))
_sym_db.RegisterMessage(FeelSensorReq)

CoorSensorReq = _reflection.GeneratedProtocolMessageType('CoorSensorReq', (_message.Message,), dict(
  DESCRIPTOR = _COORSENSORREQ,
  __module__ = 'basic_pb2'
  # @@protoc_insertion_point(class_scope:CoorSensorReq)
  ))
_sym_db.RegisterMessage(CoorSensorReq)

DeepNaviReq = _reflection.GeneratedProtocolMessageType('DeepNaviReq', (_message.Message,), dict(
  DESCRIPTOR = _DEEPNAVIREQ,
  __module__ = 'basic_pb2'
  # @@protoc_insertion_point(class_scope:DeepNaviReq)
  ))
_sym_db.RegisterMessage(DeepNaviReq)

DeepNaviRes = _reflection.GeneratedProtocolMessageType('DeepNaviRes', (_message.Message,), dict(
  DESCRIPTOR = _DEEPNAVIRES,
  __module__ = 'basic_pb2'
  # @@protoc_insertion_point(class_scope:DeepNaviRes)
  ))
_sym_db.RegisterMessage(DeepNaviRes)


DESCRIPTOR._options = None
# @@protoc_insertion_point(module_scope)
