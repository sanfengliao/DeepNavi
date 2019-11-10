package com.sysu.deepnavi.impl

import android.hardware.Sensor
import android.hardware.SensorEvent
import com.sysu.deepnavi.bean.Basic

class SensorListeners {

    private fun createCoorSensorReq(event: SensorEvent, xIndex: Int = 0, yIndex: Int = 1, zIndex: Int = 2): Basic.CoorSensorReq {
        val values = event.values
        return Basic.CoorSensorReq.newBuilder()
            .setX(values[xIndex])
            .setY(values[yIndex])
            .setZ(values[zIndex])
            .build()
    }

    val magneticListener = SensorListener2(Sensor.TYPE_MAGNETIC_FIELD, "magneticList")
    { event -> createCoorSensorReq(event) }

    val accelerometerListener = SensorListener2(Sensor.TYPE_ACCELEROMETER, "accelerometerList")
    { event -> createCoorSensorReq(event) }

    val orientationListener = SensorListener2(Sensor.TYPE_ORIENTATION, "orientationList")
    { event -> createCoorSensorReq(event, 1, 2, 0) }

    val gyroscopeListener = SensorListener2(Sensor.TYPE_GYROSCOPE, "gyroscopeList")
    { event -> createCoorSensorReq(event) }

    val gravityListener = SensorListener2(Sensor.TYPE_GRAVITY, "gravityList")
    { event -> createCoorSensorReq(event) }

    val linear_accelerationListener = SensorListener2(Sensor.TYPE_LINEAR_ACCELERATION, "linear_accelerationList")
    { event -> createCoorSensorReq(event) }

    val ambient_temperatureListener = SensorListener2<Basic.FeelSensorReq>(Sensor.TYPE_AMBIENT_TEMPERATURE, "ambient_temperatureList")
    { event -> Basic.FeelSensorReq.newBuilder().setValue(event.values[0]).build() }

    val lightListener = SensorListener2<Basic.FeelSensorReq>(Sensor.TYPE_LIGHT, "lightList")
    { event -> Basic.FeelSensorReq.newBuilder().setValue(event.values[0]).build() }

    val pressureListener = SensorListener2<Basic.FeelSensorReq>(Sensor.TYPE_PRESSURE, "pressureList")
    { event -> Basic.FeelSensorReq.newBuilder().setValue(event.values[0]).build() }

    val proximityListener = SensorListener2<Basic.FeelSensorReq>(Sensor.TYPE_PRESSURE, "proximityList")
    { event -> Basic.FeelSensorReq.newBuilder().setValue(event.values[0]).build() }

    fun initAll(rate: Int = 1000000 / 50) {
        magneticListener.init(rate)
        accelerometerListener.init(rate)
        orientationListener.init(rate)
        gyroscopeListener.init(rate)
        gravityListener.init(rate)
        linear_accelerationListener.init(rate)
        ambient_temperatureListener.init(rate)
        lightListener.init(rate)
        pressureListener.init(rate)
        proximityListener.init(rate)
    }
}

// Sensor.TYPE_TEMPERATURE  // 内部温度
// Sensor.TYPE_STEP_DETECTOR
// Sensor.TYPE_STEP_COUNTER
// Sensor.TYPE_STATIONARY_DETECT
// Sensor.TYPE_SIGNIFICANT_MOTION
// Sensor.TYPE_ROTATION_VECTOR
// Sensor.TYPE_RELATIVE_HUMIDITY
// Sensor.TYPE_POSE_6DOF
// Sensor.TYPE_MOTION_DETECT
// Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED
// Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT
// Sensor.TYPE_HEART_RATE
// Sensor.TYPE_HEART_BEAT
// Sensor.TYPE_GYROSCOPE_UNCALIBRATED
// Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR
// Sensor.TYPE_GAME_ROTATION_VECTOR
// Sensor.TYPE_DEVICE_PRIVATE_BASE
// Sensor.TYPE_ALL
// Sensor.TYPE_ACCELEROMETER_UNCALIBRATED
