package com.wire.android.ui.calling

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import androidx.appcompat.app.AppCompatActivity
import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProximitySensorManager @Inject constructor(
    private val context: Context,
    private val currentSession: CurrentSessionUseCase,
    @KaliumCoreLogic private val coreLogic: CoreLogic
) {

    private lateinit var sensorManager: SensorManager
    private var proximity: Sensor? = null
    private lateinit var wakeLock: PowerManager.WakeLock

    fun initialize() {
        sensorManager = context.getSystemService(AppCompatActivity.SENSOR_SERVICE) as SensorManager
        proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        val powerManager = context.getSystemService(AppCompatActivity.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(field, TAG);
    }

    fun registerListener() {
        proximity?.also { proximity ->
            sensorManager.registerListener(sensorEventListener, proximity, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun unRegisterListener() {
        sensorManager.unregisterListener(sensorEventListener)
    }

    private val sensorEventListener = object : SensorEventListener {

        override fun onSensorChanged(event: SensorEvent) {
            GlobalScope.launch {
                coreLogic.globalScope {
                    when (val currentSession = currentSession()) {
                        is CurrentSessionResult.Success -> {
                            val userId = currentSession.accountInfo.userId
                            val isCallRunning = coreLogic.getSessionScope(userId).calls.isCallRunning()
                            val distance = event.values.first()
                            val shouldTurnOffScreen = !wakeLock.isHeld && distance == NEAR_DISTANCE && isCallRunning
                            if (shouldTurnOffScreen) {
                                wakeLock.acquire()
                            } else if (wakeLock.isHeld) {
                                wakeLock.release()
                            }
                        }
                        else -> {
                            // NO SESSION - Nothing to do
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


