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

package com.wire.android.ui.calling.common

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import androidx.appcompat.app.AppCompatActivity
import com.wire.android.appLogger
import com.wire.android.di.ApplicationScope
import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.SingleIn

@SingleIn(AppScope::class)
class ProximitySensorManager @Inject constructor(
    private val context: Context,
    private val currentSession: Lazy<CurrentSessionUseCase>,
    @KaliumCoreLogic private val coreLogic: Lazy<CoreLogic>,
    @ApplicationScope private val appCoroutineScope: CoroutineScope
) {

    private lateinit var sensorManager: SensorManager
    private var proximity: Sensor? = null
    private lateinit var wakeLock: PowerManager.WakeLock
    private val wakeLockLock = Any()

    @Volatile
    private var isListening = false

    fun initialize() {
        sensorManager = context.getSystemService(AppCompatActivity.SENSOR_SERVICE) as SensorManager
        proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        val powerManager = context.getSystemService(AppCompatActivity.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(field, TAG).apply {
            // non-reference-counted so a redundant release() is a no-op instead of throwing "WakeLock under-locked"
            setReferenceCounted(false)
        }
    }

    fun registerListener() {
        proximity?.also { proximity ->
            isListening = true
            sensorManager.registerListener(sensorEventListener, proximity, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun unRegisterListener() {
        sensorManager.unregisterListener(sensorEventListener)
        synchronized(wakeLockLock) {
            isListening = false
            releaseWakeLock()
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    private val sensorEventListener = object : SensorEventListener {

        override fun onSensorChanged(event: SensorEvent) {
            appCoroutineScope.launch {
                coreLogic.value.globalScope {
                    val currentSession = currentSession.value.invoke()
                    when {
                        currentSession is CurrentSessionResult.Success && currentSession.accountInfo.isValid() -> {
                            val userId = currentSession.accountInfo.userId
                            val isCallRunning = coreLogic.value.getSessionScope(userId).calls.isCallRunning()
                            val distance = event.values.first()
                            val shouldTurnOffScreen = distance == NEAR_DISTANCE && isCallRunning
                            appLogger.i(
                                "$TAG onSensorChanged: isCallRunning: $isCallRunning distance: $distance " +
                                        "shouldTurnOffScreen: $shouldTurnOffScreen"
                            )
                            synchronized(wakeLockLock) {
                                // ignore events processed after the listener was unregistered
                                if (shouldTurnOffScreen && isListening) {
                                    if (!wakeLock.isHeld) {
                                        wakeLock.acquire()
                                    }
                                } else {
                                    releaseWakeLock()
                                }
                            }
                        }

                        else -> { // NO SESSION - just release in case it's still held
                            synchronized(wakeLockLock) { releaseWakeLock() }
                        }
                    }
                }
            }
        }

        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
            // Do something here if sensor accuracy changes.
        }
    }

    companion object {
        const val TAG = "calling:ProximitySensorManager"
        const val field = 0x00000020
        const val NEAR_DISTANCE = 0F
    }
}
