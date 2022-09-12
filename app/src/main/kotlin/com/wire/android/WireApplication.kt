package com.wire.android

import android.app.Application
import android.os.Build
import androidx.work.Configuration
import co.touchlab.kermit.platformLogWriter
import com.datadog.android.Datadog
import com.datadog.android.DatadogSite
import com.datadog.android.core.configuration.Credentials
import com.datadog.android.privacy.TrackingConsent
import com.datadog.android.rum.GlobalRum
import com.datadog.android.rum.RumMonitor
import com.google.firebase.FirebaseApp
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.util.DataDogLogger
import com.wire.android.util.LogFileWriter
import com.wire.android.util.extension.isGoogleServicesAvailable
import com.wire.android.util.getDeviceId
import com.wire.android.util.lifecycle.ConnectionPolicyManager
import com.wire.kalium.logger.KaliumLogLevel
import com.wire.kalium.logger.KaliumLogger
import com.wire.kalium.logic.CoreLogger
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.sync.WrapperWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Indicates whether the build is private (dev || internal) or public
 */

var appLogger = KaliumLogger(
    config = KaliumLogger.Config(
        severity = if (BuildConfig.PRIVATE_BUILD) KaliumLogLevel.DEBUG else KaliumLogLevel.DISABLED,
        tag = "WireAppLogger"
    ),
    DataDogLogger,
    platformLogWriter()
)

@HiltAndroidApp
class WireApplication : Application(), Configuration.Provider {

    @Inject
    @KaliumCoreLogic
    lateinit var coreLogic: CoreLogic

    @Inject
    lateinit var logFileWriter: LogFileWriter

    @Inject
    lateinit var connectionPolicyManager: ConnectionPolicyManager

    override fun getWorkManagerConfiguration(): Configuration {
        val myWorkerFactory = WrapperWorkerFactory(coreLogic)
        return Configuration.Builder()
            .setWorkerFactory(myWorkerFactory)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        if (this.isGoogleServicesAvailable()) {
            FirebaseApp.initializeApp(this)
        }

        enableDatadog()

        if (BuildConfig.PRIVATE_BUILD || coreLogic.getGlobalScope().isLoggingEnabled()) {
            enableLoggingAndInitiateFileLogging()
        }

        // TODO: Can be handled in one of Sync steps
        coreLogic.updateApiVersionsScheduler.schedulePeriodicApiVersionUpdate()

        connectionPolicyManager.startObservingAppLifecycle()

        logDeviceInformation()
    }

    private fun logDeviceInformation() {
        appLogger.d(
            "Device info: App version=${BuildConfig.VERSION_NAME} " +
                    "| OS Version=${Build.VERSION.SDK_INT} " +
                    "| Phone Model=${Build.BRAND}/${Build.MODEL}"
        )
    }

    private fun enableLoggingAndInitiateFileLogging() {
        CoreLogger.setLoggingLevel(
            level = KaliumLogLevel.VERBOSE,
            logWriters = arrayOf(DataDogLogger, platformLogWriter())
        )
        logFileWriter.start()
        appLogger.i("Logger enabled")
    }

    private fun enableDatadog() {

        val clientToken = "pub98ad02250435b6082337bb79f66cbc19"
        val applicationId = "619af3ef-2fa6-41e2-8bb1-b42041d50802"

        val environmentName = "internal"
        val appVariantName = "com.wire.android.dev.debug"

        val configuration = com.datadog.android.core.configuration.Configuration.Builder(
            logsEnabled = true,
            tracesEnabled = true,
            rumEnabled = true,
            crashReportsEnabled = true,
        ).trackInteractions()
            .trackLongTasks(LONG_TASK_THRESH_HOLD_MS)
            .useSite(DatadogSite.EU1)
            .build()

        val credentials = Credentials(clientToken, environmentName, appVariantName, applicationId)
        Datadog.initialize(this, credentials, configuration, TrackingConsent.GRANTED)
        Datadog.setUserInfo(id = getDeviceId(this))
        GlobalRum.registerIfAbsent(RumMonitor.Builder().build())
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        appLogger.w("onTrimMemory called - App info: Memory trim level=${MemoryLevel.byLevel(level)}")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        appLogger.w("onLowMemory called - Stopping logging, buckling the seatbelt and hoping for the best!")
        logFileWriter.stop()
    }

    private companion object {
        const val LONG_TASK_THRESH_HOLD_MS = 1000L

        @Suppress("MagicNumber")
        enum class MemoryLevel(val level: Int) {
            TRIM_MEMORY_BACKGROUND(40),
            TRIM_MEMORY_COMPLETE(80),
            TRIM_MEMORY_MODERATE(60),
            TRIM_MEMORY_RUNNING_CRITICAL(15),
            TRIM_MEMORY_RUNNING_LOW(10),
            TRIM_MEMORY_RUNNING_MODERATE(5),
            TRIM_MEMORY_UI_HIDDEN(20);

            companion object {
                fun byLevel(value: Int) = values().firstOrNull { it.level == value }
            }
        }
    }
}
