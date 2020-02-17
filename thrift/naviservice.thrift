struct CoorSensor {
  1: required double x
  2: required double y
  3: required double z
}


struct FeelSensor {
  1: required double x
}

struct NaviModel {
  1: binary image
  2: list<CoorSensor> magneticList // 磁场
  3: list<CoorSensor> accelerometerList // 加速度
  4: list<CoorSensor> orientationList // 方向
  5: list<CoorSensor> gyroscopeList // 陀螺仪
  6: list<CoorSensor> gravityList // 重力
  7: list<CoorSensor> linearAccelerationList // 线性加速度
  8: list<FeelSensor> ambientTemperatureList // 温度
  9: list<FeelSensor> lightList  // 亮度
  10: list<FeelSensor> pressureList  // 压力
  11: list<FeelSensor> proximityList  // 近距离
  12: list<i32> wifiList
}

service NaviModelService {
  list<double> predictByImageAndMag(1: NaviModel naviModel),
  list<double> predictByImageAndWifi(1: NaviModel naviModel)
}