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
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProximitySensorManager @Inject constructor(
    private val context: Context,
    private val currentSession: Lazy<CurrentSessionUseCase>,
    @KaliumCoreLogic private val coreLogic: Lazy<CoreLogic>,
    @ApplicationScope private val appCoroutineScope: CoroutineScope
) {

    private lateinit var sensorManager: SensorManager
    private var proximity: Sensor? = null
    private lateinit var wakeLock: PowerManager.WakeLock

    fun initialize() {
        sensorManager = context.getSystemService(AppCompatActivity.SENSOR_SERVICE) as SensorManager
        proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        val powerManager = context.getSystemService(AppCompatActivity.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(field, TAG)
    }

    fun registerListener() {
        proximity?.also { proximity ->
            sensorManager.registerListener(sensorEventListener, proximity, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun unRegisterListener() {
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        sensorManager.unregisterListener(sensorEventListener)
    }

    private val sensorEventListener = object : SensorEventListener {

        override fun onSensorChanged(event: SensorEvent) {
            appCoroutineScope.launch {
                coreLogic.get().globalScope {
                    val currentSession = currentSession.get().invoke()
                    when {
                        currentSession is CurrentSessionResult.Success && currentSession.accountInfo.isValid() -> {
                            val userId = currentSession.accountInfo.userId
                            val isCallRunning = coreLogic.get().getSessionScope(userId).calls.isCallRunning()
                            val distance = event.values.first()
                            val shouldTurnOffScreen = distance == NEAR_DISTANCE && isCallRunning
                            appLogger.i(
                                "$TAG onSensorChanged: isCallRunning: $isCallRunning distance: $distance " +
                                        "shouldTurnOffScreen: $shouldTurnOffScreen"
                            )
                            if (shouldTurnOffScreen) {
                                if (!wakeLock.isHeld) {
                                    wakeLock.acquire()
                                }
                            } else {
                                if (wakeLock.isHeld) {
                                    wakeLock.release()
                                }
                            }
                        }

                        else -> { // NO SESSION - just release in case it's still held
                            if (wakeLock.isHeld) {
                                wakeLock.release()
                            }
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
