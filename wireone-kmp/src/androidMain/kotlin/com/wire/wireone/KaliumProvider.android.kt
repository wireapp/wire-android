package com.wire.wireone

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.featureFlags.KaliumConfigs

@Composable
internal actual fun rememberKaliumProvider(): KaliumProvider {
    val appContext = LocalContext.current.applicationContext
    return remember(appContext) {
        CoreLogicKaliumProvider(
            coreLogicFactory = {
                CoreLogic(
                    userAgent = "wireone-android",
                    appContext = appContext,
                    rootPath = appContext.filesDir.resolve("wireone").absolutePath,
                    kaliumConfigs = KaliumConfigs(),
                )
            },
            readyLine = "Kalium: initialized on Android"
        )
    }
}
