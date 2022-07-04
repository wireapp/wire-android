package com.wire.android

import android.app.Application
import androidx.work.Configuration
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import com.google.firebase.FirebaseApp
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.util.KaliumFileWriter
import com.wire.android.util.extension.isGoogleServicesAvailable
import com.wire.kalium.logger.KaliumLogLevel
import com.wire.kalium.logger.KaliumLogger
import com.wire.kalium.logic.CoreLogger
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.sync.WrapperWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

private const val flavor = BuildConfig.FLAVOR

var appLogger = KaliumLogger(
    config = KaliumLogger.Config(
        severity = if (
            flavor.startsWith("Dev", true) || flavor.startsWith("Internal", true)
        ) KaliumLogLevel.DEBUG else KaliumLogLevel.DISABLED,
        tag = "WireAppLogger"
    )
)

@HiltAndroidApp
class WireApplication : Application(), Configuration.Provider {

    @Inject
    @KaliumCoreLogic
    lateinit var coreLogic: CoreLogic

    @Inject
    lateinit var kaliumFileWriter: KaliumFileWriter

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
        if (BuildConfig.FLAVOR in setOf("internal", "dev") || coreLogic.getGlobalScope().isLoggingEnabled()) {
            enableLoggingAndInitiateFileLogging()
        }

        coreLogic.updateApiVersionsScheduler.schedulePeriodicApiVersionUpdate()
    }

    private fun enableLoggingAndInitiateFileLogging() {
        CoreLogger.setLoggingLevel(level = KaliumLogLevel.DEBUG)
        kaliumFileWriter.start()
        appLogger.i("logged enabled")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        appLogger.w("onLowMemory called - Stopping logging, buckling the seatbelt and hoping for the best!")
        kaliumFileWriter.stop()
    }
}
