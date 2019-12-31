package com.sysu.deepnavi.impl

import android.hardware.Sensor
import android.hardware.SensorEvent
import com.sysu.deepnavi.bean.Basic

@Suppress("MemberVisibilityCanBePrivate", "unused", "DEPRECATION")
class SensorListeners {

    companion object {
        val DEFAULT_VALUE_SENSOR_CONFIG = Basic.DeepNaviReq.getDescriptor().fields.filter { it != null && it.name != "time" }.map { it.name }.toHashSet()
    }

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

    val linearAccelerationListener = SensorListener2(Sensor.TYPE_LINEAR_ACCELERATION, "linearAccelerationList")
    { event -> createCoorSensorReq(event) }

    val ambientTemperatureListener = SensorListener2<Basic.FeelSensorReq>(Sensor.TYPE_AMBIENT_TEMPERATURE, "ambientTemperatureList")
    { event -> Basic.FeelSensorReq.newBuilder().setValue(event.values[0]).build() }

    val lightListener = SensorListener2<Basic.FeelSensorReq>(Sensor.TYPE_LIGHT, "lightList")
    { event -> Basic.FeelSensorReq.newBuilder().setValue(event.values[0]).build() }

    val pressureListener = SensorListener2<Basic.FeelSensorReq>(Sensor.TYPE_PRESSURE, "pressureList")
    { event -> Basic.FeelSensorReq.newBuilder().setValue(event.values[0]).build() }

    val proximityListener = SensorListener2<Basic.FeelSensorReq>(Sensor.TYPE_PRESSURE, "proximityList")
    { event -> Basic.FeelSensorReq.newBuilder().setValue(event.values[0]).build() }

    fun initAll(
        rate: Int = 1000000 / 50,
        registerList: List<Boolean> = (0 until 10).map { true },
        useList: Set<String> = DEFAULT_VALUE_SENSOR_CONFIG
    ) {
        if (magneticListener.field in useList) {
            magneticListener.init(rate, registerList[0])
        }
        if (accelerometerListener.field in useList) {
            accelerometerListener.init(rate, registerList[1])
        }
        if (orientationListener.field in useList) {
            orientationListener.init(rate, registerList[2])
        }
        if (gyroscopeListener.field in useList) {
            gyroscopeListener.init(rate, registerList[3])
        }
        if (gyroscopeListener.field in useList) {
            gravityListener.init(rate, registerList[4])
        }
        if (linearAccelerationListener.field in useList) {
            linearAccelerationListener.init(rate, registerList[5])
        }
        if (ambientTemperatureListener.field in useList) {
            ambientTemperatureListener.init(rate, registerList[6])
        }
        if (lightListener.field in useList) {
            lightListener.init(rate, registerList[7])
        }
        if (pressureListener.field in useList) {
            pressureListener.init(rate, registerList[8])
        }
        if (proximityListener.field in useList) {
            proximityListener.init(rate, registerList[9])
        }
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
