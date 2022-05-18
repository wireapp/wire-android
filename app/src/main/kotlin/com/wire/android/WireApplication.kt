package com.wire.android

import android.app.Application
import androidx.work.Configuration
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.util.KaliumFileWriter
import com.wire.android.util.WireConstants.FB_PROD_API_KEY
import com.wire.android.util.WireConstants.FB_PROD_APPLICATION_ID1
import com.wire.android.util.WireConstants.FB_PROD_APPLICATION_ID2
import com.wire.android.util.WireConstants.FB_PROD_APPLICATION_ID3
import com.wire.android.util.WireConstants.FB_PROD_APPLICATION_ID4
import com.wire.android.util.WireConstants.FB_PROD_APPLICATION_ID5
import com.wire.android.util.WireConstants.FB_PROD_PROJECT_ID
import com.wire.android.util.WireConstants.FB_PROD_SENDER_ID
import com.wire.android.util.WireConstants.FB_STAGING_API_KEY
import com.wire.android.util.WireConstants.FB_STAGING_APPLICATION_ID
import com.wire.android.util.WireConstants.FB_STAGING_PROJECT_ID
import com.wire.android.util.WireConstants.FB_STAGING_SENDER_ID
import com.wire.android.util.WireConstants.ServerTitle
import com.wire.android.util.extension.isGoogleServicesAvailable
import com.wire.kalium.logger.KaliumLogLevel
import com.wire.kalium.logger.KaliumLogger
import com.wire.kalium.logic.CoreLogger
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.ServerConfig
import com.wire.kalium.logic.sync.WrapperWorkerFactory
import dagger.hilt.android.HiltAndroidApp
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

    override fun getWorkManagerConfiguration(): Configuration {
        val myWorkerFactory = WrapperWorkerFactory(coreLogic)

        return Configuration.Builder()
            .setWorkerFactory(myWorkerFactory)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        if (this.isGoogleServicesAvailable()) {
            val serverConfig: ServerConfig = ServerConfig.DEFAULT

            when (serverConfig.title.lowercase()) {
                ServerTitle.STAGING.title, ServerTitle.QA_DEMO.title,
                ServerTitle.ANTA.title, ServerTitle.BELLA.title,
                ServerTitle.CHALA.title -> {
                    FirebaseApp.initializeApp(
                        this, FirebaseOptions.Builder()
                            .setProjectId(FB_STAGING_PROJECT_ID)
                            .setApplicationId(FB_STAGING_APPLICATION_ID)
                            .setApiKey(FB_STAGING_API_KEY)
                            .setGcmSenderId(FB_STAGING_SENDER_ID)
                            .build()
                    )
                }
                else -> {
                    FirebaseApp.initializeApp(
                        this, FirebaseOptions.Builder()
                            .setProjectId(FB_PROD_PROJECT_ID)
                            .setApplicationId(FB_PROD_APPLICATION_ID1)
                            .setApplicationId(FB_PROD_APPLICATION_ID2)
                            .setApplicationId(FB_PROD_APPLICATION_ID3)
                            .setApplicationId(FB_PROD_APPLICATION_ID4)
                            .setApplicationId(FB_PROD_APPLICATION_ID5)
                            .setApiKey(FB_PROD_API_KEY)
                            .setGcmSenderId(FB_PROD_SENDER_ID)
                            .build()
                    )
                }
            }
        }
        if (BuildConfig.DEBUG || coreLogic.getAuthenticationScope().isLoggingEnabled()) {
            enableLoggingAndInitiateFileLogging()
        }
    }

    private fun enableLoggingAndInitiateFileLogging() {
        kaliumFileWriter.init(applicationContext.cacheDir.absolutePath)
        CoreLogger.setLoggingLevel(
            level = KaliumLogLevel.DEBUG, kaliumFileWriter
        )
        appLogger.i("logged enabled")
    }
}
