package com.wire.android

import android.app.Application
import androidx.work.Configuration
import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.sync.WrapperWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import com.wire.kalium.logic.configuration.ServerConfig

@HiltAndroidApp
class WireApplication : Application(), Configuration.Provider {

    @Inject
    @KaliumCoreLogic
    lateinit var coreLogic: CoreLogic
companion object{
    var serverConfig = ServerConfig.STAGING
}

    override fun getWorkManagerConfiguration(): Configuration {
        val myWorkerFactory = WrapperWorkerFactory(coreLogic)

        return Configuration.Builder()
            .setWorkerFactory(myWorkerFactory)
            .build()
    }
}
