struct Coor {
    1: required double x
    2: required double y
    3: required double z
}


struct FeelSensor {
    1: required double value
}

struct NaviModel {
    1: binary image
    2: list<Coor> magneticList // 磁场
    3: list<Coor> accelerometerList // 加速度
    4: list<Coor> orientationList // 方向
    5: list<Coor> gyroscopeList // 陀螺仪
    6: list<Coor> gravityList // 重力
    7: list<Coor> linearAccelerationList // 线性加速度
    8: list<FeelSensor> ambientTemperatureList // 温度
    9: list<FeelSensor> lightList  // 亮度
    10: list<FeelSensor> pressureList  // 压力
    11: list<FeelSensor> proximityList  // 近距离
    12: list<i32> wifiList
    13: string modelPath
}

struct LocationResult {
    1: Coor coor
    2: double rotation
}

service NaviModelService {
    LocationResult predict(1: NaviModel model)
}