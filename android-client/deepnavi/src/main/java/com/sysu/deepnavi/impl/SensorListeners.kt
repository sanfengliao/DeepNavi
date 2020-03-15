package com.sysu.deepnavi.impl

import android.hardware.Sensor
import android.hardware.SensorEvent
import com.sysu.deepnavi.DeepNaviManager
import com.sysu.deepnavi.bean.Basic

@Suppress("MemberVisibilityCanBePrivate", "unused", "DEPRECATION")
object SensorListeners {
    val DEFAULT_VALUE_SENSOR_CONFIG = Basic.DeepNaviReq.getDescriptor().fields.filter { it != null && it.name != "time" }.map { it.name }.toHashSet()

    private fun createCoorSensorReq(event: SensorEvent, xIndex: Int = 0, yIndex: Int = 1, zIndex: Int = 2): Basic.Coor {
        val values = event.values
        return Basic.Coor.newBuilder()
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

    fun initAll(useList: Set<String> = DEFAULT_VALUE_SENSOR_CONFIG) {
        if (magneticListener.field in useList) {
            magneticListener.init()
        }
        if (accelerometerListener.field in useList) {
            accelerometerListener.init()
        }
        if (orientationListener.field in useList) {
            orientationListener.init()
        }
        if (gyroscopeListener.field in useList) {
            gyroscopeListener.init()
        }
        if (gyroscopeListener.field in useList) {
            gravityListener.init()
        }
        if (linearAccelerationListener.field in useList) {
            linearAccelerationListener.init()
        }
        if (ambientTemperatureListener.field in useList) {
            ambientTemperatureListener.init()
        }
        if (lightListener.field in useList) {
            lightListener.init()
        }
        if (pressureListener.field in useList) {
            pressureListener.init()
        }
        if (proximityListener.field in useList) {
            proximityListener.init()
        }
    }

    fun registerAll(rate: Int = 1000000 / 50, registerList: Set<String> = DEFAULT_VALUE_SENSOR_CONFIG) {
        if (magneticListener.field in registerList) {
            magneticListener.register(rate)
        }
        if (accelerometerListener.field in registerList) {
            accelerometerListener.register(rate)
        }
        if (orientationListener.field in registerList) {
            orientationListener.register(rate)
        }
        if (gyroscopeListener.field in registerList) {
            gyroscopeListener.register(rate)
        }
        if (gravityListener.field in registerList) {
            gravityListener.register(rate)
        }
        if (linearAccelerationListener.field in registerList) {
            linearAccelerationListener.register(rate)
        }
        if (ambientTemperatureListener.field in registerList) {
            ambientTemperatureListener.register(rate)
        }
        if (lightListener.field in registerList) {
            lightListener.register(rate)
        }
        if (pressureListener.field in registerList) {
            pressureListener.register(rate)
        }
        if (proximityListener.field in registerList) {
            proximityListener.register(rate)
        }
    }

    fun registerAll(
        rates: Map<String, Int> = DEFAULT_VALUE_SENSOR_CONFIG.map { it to 1000000 / 50 }.toMap(),
        registerList: Set<String> = DEFAULT_VALUE_SENSOR_CONFIG) {
        val default = 1000000 / 50
        if (magneticListener.field in registerList) {
            magneticListener.register(rates[magneticListener.field] ?: default)
        }
        if (accelerometerListener.field in registerList) {
            accelerometerListener.register(rates[accelerometerListener.field] ?: default)
        }
        if (orientationListener.field in registerList) {
            orientationListener.register(rates[orientationListener.field] ?: default)
        }
        if (gyroscopeListener.field in registerList) {
            gyroscopeListener.register(rates[gyroscopeListener.field] ?: default)
        }
        if (gravityListener.field in registerList) {
            gravityListener.register(rates[gravityListener.field] ?: default)
        }
        if (linearAccelerationListener.field in registerList) {
            linearAccelerationListener.register(rates[linearAccelerationListener.field] ?: default)
        }
        if (ambientTemperatureListener.field in registerList) {
            ambientTemperatureListener.register(rates[ambientTemperatureListener.field] ?: default)
        }
        if (lightListener.field in registerList) {
            lightListener.register(rates[lightListener.field] ?: default)
        }
        if (pressureListener.field in registerList) {
            pressureListener.register(rates[pressureListener.field] ?: default)
        }
        if (proximityListener.field in registerList) {
            proximityListener.register(rates[proximityListener.field] ?: default)
        }
    }

    fun unregisterAll() {
        DeepNaviManager.get().unregisterListener()
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
