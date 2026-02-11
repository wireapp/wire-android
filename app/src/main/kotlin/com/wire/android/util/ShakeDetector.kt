/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlin.math.sqrt

class ShakeDetector(
    context: Context,
    private val shakeThresholdGravity: Float = DEFAULT_SHAKE_THRESHOLD_GRAVITY,
    private val debounceMs: Long = DEFAULT_DEBOUNCE_MS,
    private val startupIgnoreMs: Long = DEFAULT_STARTUP_IGNORE_MS,
    private val shakeSlopMs: Long = DEFAULT_SHAKE_SLOP_MS,
    private val shakeCountResetMs: Long = DEFAULT_SHAKE_COUNT_RESET_MS,
    private val requiredShakes: Int = DEFAULT_REQUIRED_SHAKES
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var startTimestamp = 0L
    private var lastShakeTimestamp = 0L
    private var lastEmitTimestamp = 0L
    private var shakeCount = 0
    private val shakeEvents = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    fun observeShakes(): Flow<Unit> = shakeEvents

    fun start() {
        if (accelerometer != null) {
            startTimestamp = SystemClock.elapsedRealtime()
            lastShakeTimestamp = 0L
            lastEmitTimestamp = 0L
            shakeCount = 0
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0] / SensorManager.GRAVITY_EARTH
            val y = event.values[1] / SensorManager.GRAVITY_EARTH
            val z = event.values[2] / SensorManager.GRAVITY_EARTH

            val gForce = sqrt(x * x + y * y + z * z)
            if (gForce >= shakeThresholdGravity) {
                val now = SystemClock.elapsedRealtime()
                if (now - startTimestamp < startupIgnoreMs) return
                if (now - lastShakeTimestamp < shakeSlopMs) return

                if (now - lastShakeTimestamp > shakeCountResetMs) {
                    shakeCount = 0
                }

                lastShakeTimestamp = now
                shakeCount += 1

                if (shakeCount >= requiredShakes && now - lastEmitTimestamp >= debounceMs) {
                    lastEmitTimestamp = now
                    shakeCount = 0
                    shakeEvents.tryEmit(Unit)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private const val DEFAULT_SHAKE_THRESHOLD_GRAVITY = 3.0f
        private const val DEFAULT_DEBOUNCE_MS = 2000L
        private const val DEFAULT_STARTUP_IGNORE_MS = 1500L
        private const val DEFAULT_SHAKE_SLOP_MS = 250L
        private const val DEFAULT_SHAKE_COUNT_RESET_MS = 1000L
        private const val DEFAULT_REQUIRED_SHAKES = 2
    }
}
