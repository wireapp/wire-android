@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.wire.wireone

import com.wire.kalium.common.logger.CoreLogger
import com.wire.kalium.logger.KaliumLogLevel
import com.wire.kalium.logger.KaliumLogger
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import co.touchlab.kermit.platformLogWriter
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

private object IosKaliumProvider : KaliumProvider {
    private var appLogger = KaliumLogger.disabled()

    private val coreLogic: CoreLogic by lazy {
        val config = KaliumLogger.Config(
            initialLevel = KaliumLogLevel.VERBOSE,
            initialLogWriterList = listOf(platformLogWriter())
        )
        CoreLogger.init(config)
        appLogger = KaliumLogger(config = config, tag = "WireOne")
        appLogger.i("iOS Kalium logging initialized")
        CoreLogic(
            rootPath = persistentRootPath(),
            kaliumConfigs = KaliumConfigs(),
            userAgent = "wireone-ios",
            useInMemoryStorage = false,
        )
    }

    override fun statusLine(): String {
        coreLogic
        return "Kalium: initialized on iOS"
    }
}

internal actual fun createKaliumProvider(): KaliumProvider = IosKaliumProvider

private fun persistentRootPath(): String {
    val paths = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)
    val basePath = paths.firstOrNull() as? String ?: ""
    val appSupportPath = if (basePath.isNotEmpty()) "$basePath/wireone" else ""
    if (appSupportPath.isNotEmpty()) {
        NSFileManager.defaultManager.createDirectoryAtPath(
            path = appSupportPath,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )
    }
    return appSupportPath
}
