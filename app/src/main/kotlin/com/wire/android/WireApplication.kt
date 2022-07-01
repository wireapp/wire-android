package com.wire.android

import android.app.Application
import androidx.work.Configuration
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

private val flavor = BuildConfig.FLAVOR
val kaliumFileWriter = KaliumFileWriter()
var appLogger = KaliumLogger(
    config = KaliumLogger.Config(
        severity = if (
            flavor.startsWith("Dev", true) || flavor.startsWith("Internal", true)
        ) KaliumLogLevel.DEBUG else KaliumLogLevel.DISABLED,
        tag = "WireAppLogger"
    ), kaliumFileWriter
)

@HiltAndroidApp
class WireApplication : Application(), Configuration.Provider {

    @Inject
    @KaliumCoreLogic
    lateinit var coreLogic: CoreLogic

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
        applicationScope.launch {
            kaliumFileWriter.init(applicationContext.cacheDir.absolutePath)
            CoreLogger.setLoggingLevel(
                level = KaliumLogLevel.DEBUG, kaliumFileWriter
            )
            appLogger.i("logged enabled")
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        applicationScope.cancel()
    }
}
