package com.wire.android

import android.app.Application
import androidx.work.Configuration
import com.google.firebase.FirebaseApp
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.util.extension.isGoogleServicesAvailable
import com.wire.kalium.logger.KaliumLogLevel
import com.wire.kalium.logger.KaliumLogger
import com.wire.kalium.logic.CoreLogger
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.sync.WrapperWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

private val flavor = BuildConfig.FLAVOR
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
        if (BuildConfig.DEBUG) {
            CoreLogger.setLoggingLevel(
                level = KaliumLogLevel.DEBUG
            )
        }
    }
}
