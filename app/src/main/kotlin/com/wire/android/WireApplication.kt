/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android

import android.app.Application
import android.content.ComponentCallbacks2
import android.os.Build
import android.os.StrictMode
import androidx.work.Configuration
import co.touchlab.kermit.platformLogWriter
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.di.ApplicationScope
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.util.DataDogLogger
import com.wire.android.util.LogFileWriter
import com.wire.android.util.extension.isGoogleServicesAvailable
import com.wire.android.util.getGitBuildId
import com.wire.android.util.lifecycle.ConnectionPolicyManager
import com.wire.android.workmanager.WireWorkerFactory
import com.wire.kalium.logger.KaliumLogLevel
import com.wire.kalium.logger.KaliumLogger
import com.wire.kalium.logic.CoreLogger
import com.wire.kalium.logic.CoreLogic
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

// App wide global logger, carefully initialized when our application is "onCreate"
var appLogger: KaliumLogger = KaliumLogger.disabled()

@HiltAndroidApp
class WireApplication : Application(), Configuration.Provider {

    @Inject
    @KaliumCoreLogic
    lateinit var coreLogic: CoreLogic

    @Inject
    lateinit var logFileWriter: LogFileWriter

    @Inject
    lateinit var connectionPolicyManager: ConnectionPolicyManager

    @Inject
    lateinit var wireWorkerFactory: WireWorkerFactory

    @Inject
    lateinit var globalObserversManager: GlobalObserversManager

    @Inject
    lateinit var globalDataStore: GlobalDataStore

    @Inject
    @ApplicationScope
    lateinit var globalAppScope: CoroutineScope
    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(wireWorkerFactory)
            .build()
    }

    override fun onCreate() {
        super.onCreate()

        enableStrictMode()

        if (this.isGoogleServicesAvailable()) {
            val firebaseOptions = FirebaseOptions.Builder()
                .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                .setGcmSenderId(BuildConfig.FIREBASE_PUSH_SENDER_ID)
                .setApiKey(BuildConfig.GOOGLE_API_KEY)
                .setProjectId(BuildConfig.FCM_PROJECT_ID)
                .build()
            FirebaseApp.initializeApp(this, firebaseOptions)
        }

        initializeApplicationLoggingFrameworks()
        connectionPolicyManager.startObservingAppLifecycle()

        // TODO: Can be handled in one of Sync steps
        coreLogic.updateApiVersionsScheduler.schedulePeriodicApiVersionUpdate()

        globalObserversManager.observe()
    }

    private fun enableStrictMode() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .penaltyLog()
//                    .penaltyDeath() // Disabled as some devices and libraries are not compliant
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    // .penaltyDeath() TODO: add it later after fixing reported violations
                    .build()
            )
        }
    }

    private fun initializeApplicationLoggingFrameworks() {
        globalAppScope.launch {
            // 1. Datadog should be initialized first
            ExternalLoggerManager.initDatadogLogger(applicationContext, globalDataStore)
            // 2. Initialize our internal logging framework
            appLogger = KaliumLogger(
                config = KaliumLogger.Config(
                    severity = if (BuildConfig.PRIVATE_BUILD) KaliumLogLevel.DEBUG else KaliumLogLevel.DISABLED,
                    tag = "WireAppLogger"
                ),
                DataDogLogger,
                platformLogWriter()
            )
            // 3. Initialize our internal FILE logging framework
            enableLoggingAndInitiateFileLogging()
            // 4. Everything ready, now we can log device info
            logDeviceInformation()
        }
    }

    private fun logDeviceInformation() {
        appLogger.d(
            """
            > Device info: 
                App version=${BuildConfig.VERSION_NAME} 
                OS version=${Build.VERSION.SDK_INT}
                Phone model=${Build.BRAND}/${Build.MODEL}
                Commit hash=${applicationContext.getGitBuildId()}
        """.trimIndent()
        )
    }

    private fun enableLoggingAndInitiateFileLogging() {
        globalAppScope.launch {
            if (globalDataStore.isLoggingEnabled().first()) {
                CoreLogger.setLoggingLevel(
                    level = KaliumLogLevel.VERBOSE,
                    logWriters = arrayOf(DataDogLogger, platformLogWriter())
                )
                logFileWriter.start()
                appLogger.i("Logger enabled")
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        appLogger.w(
            "onTrimMemory called - App info: Memory trim level=${MemoryLevel.byLevel(level)}. " +
                    "See more at https://developer.android.com/reference/kotlin/android/content/ComponentCallbacks2"
        )
    }

    override fun onLowMemory() {
        super.onLowMemory()
        appLogger.w("onLowMemory called - Stopping logging, buckling the seatbelt and hoping for the best!")
        logFileWriter.stop()
    }

    private companion object {
        enum class MemoryLevel(val level: Int) {
            TRIM_MEMORY_BACKGROUND(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND),
            TRIM_MEMORY_COMPLETE(ComponentCallbacks2.TRIM_MEMORY_COMPLETE),
            TRIM_MEMORY_MODERATE(ComponentCallbacks2.TRIM_MEMORY_MODERATE),
            TRIM_MEMORY_RUNNING_CRITICAL(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL),
            TRIM_MEMORY_RUNNING_LOW(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW),
            TRIM_MEMORY_RUNNING_MODERATE(ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE),
            TRIM_MEMORY_UI_HIDDEN(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN),

            @Suppress("MagicNumber")
            TRIM_MEMORY_UNKNOWN(-1);

            companion object {
                fun byLevel(value: Int) =
                    values().firstOrNull { it.level == value } ?: TRIM_MEMORY_UNKNOWN
            }
        }
    }
}
