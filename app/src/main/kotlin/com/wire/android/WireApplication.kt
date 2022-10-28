package com.wire.android

import android.app.Application
import android.content.ComponentCallbacks2
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
import com.wire.android.util.sha256
import com.wire.android.workmanager.WireWorkerFactory
import com.wire.kalium.logger.KaliumLogLevel
import com.wire.kalium.logger.KaliumLogger
import com.wire.kalium.logic.CoreLogger
import com.wire.kalium.logic.CoreLogic
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

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
    lateinit var wireWorkerFactory: WireWorkerFactory

    @Inject
    lateinit var connectionPolicyManager: ConnectionPolicyManager

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(wireWorkerFactory)
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
        Datadog.setUserInfo(id = getDeviceId()?.sha256())
        GlobalRum.registerIfAbsent(RumMonitor.Builder().build())
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
        const val LONG_TASK_THRESH_HOLD_MS = 1000L

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
                fun byLevel(value: Int) = values().firstOrNull { it.level == value } ?: TRIM_MEMORY_UNKNOWN
            }
        }
    }
}
