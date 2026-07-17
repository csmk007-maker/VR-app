package com.panovr.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.Matrix
import android.os.Build
import android.view.Surface
import android.view.WindowManager

class GyroscopeSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.display
    } else {
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay
    }

    private val rotationMatrix = FloatArray(16)
    private val remappedMatrix = FloatArray(16)

    var onGyroChanged: ((FloatArray) -> Unit)? = null

    init {
        Matrix.setIdentityM(rotationMatrix, 0)
        Matrix.setIdentityM(remappedMatrix, 0)
    }

    fun start() {
        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

            val displayRotation = display?.rotation ?: Surface.ROTATION_0

            var axisX = SensorManager.AXIS_X
            var axisY = SensorManager.AXIS_Y

            when (displayRotation) {
                Surface.ROTATION_0 -> {
                    axisX = SensorManager.AXIS_X
                    axisY = SensorManager.AXIS_Y
                }
                Surface.ROTATION_90 -> {
                    axisX = SensorManager.AXIS_Y
                    axisY = SensorManager.AXIS_MINUS_X
                }
                Surface.ROTATION_180 -> {
                    axisX = SensorManager.AXIS_MINUS_X
                    axisY = SensorManager.AXIS_MINUS_Y
                }
                Surface.ROTATION_270 -> {
                    axisX = SensorManager.AXIS_MINUS_Y
                    axisY = SensorManager.AXIS_X
                }
            }

            SensorManager.remapCoordinateSystem(rotationMatrix, axisX, axisY, remappedMatrix)

            onGyroChanged?.invoke(remappedMatrix)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }
}
