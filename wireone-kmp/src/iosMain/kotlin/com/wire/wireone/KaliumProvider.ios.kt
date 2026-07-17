@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.wire.wireone

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.wire.kalium.common.logger.CoreLogger
import com.wire.kalium.logger.KaliumLogLevel
import com.wire.kalium.logger.KaliumLogger
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import co.touchlab.kermit.platformLogWriter
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.URLByAppendingPathComponent

private val iosKaliumProvider by lazy {
    CoreLogicKaliumProvider(
        coreLogicFactory = {
            val config = KaliumLogger.Config(
                initialLevel = KaliumLogLevel.VERBOSE,
                initialLogWriterList = listOf(platformLogWriter())
            )
            CoreLogger.init(config)
            KaliumLogger(config = config, tag = "WireOne").i("iOS Kalium logging initialized")
            CoreLogic(
                rootPath = persistentRootPath(),
                kaliumConfigs = KaliumConfigs(),
                userAgent = "wireone-ios",
                useInMemoryStorage = false,
            )
        },
        readyLine = "Kalium: initialized on iOS",
        serverLinks = ServerConfig.PRODUCTION
    )
}

@Composable
internal actual fun rememberKaliumProvider(): KaliumProvider = remember { iosKaliumProvider }

private fun persistentRootPath(): String {
    val appGroupIdentifier = (NSBundle.mainBundle.objectForInfoDictionaryKey("WireGroupId") as? String)
        ?.let { "group.$it" }

    val sharedContainerPath = appGroupIdentifier
        ?.let { NSFileManager.defaultManager.containerURLForSecurityApplicationGroupIdentifier(it) }
        ?.URLByAppendingPathComponent("wireone", isDirectory = true)
        ?.path

    val rootPath = when {
        !sharedContainerPath.isNullOrBlank() -> sharedContainerPath
        else -> {
            val paths = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)
            val basePath = paths.firstOrNull() as? String ?: ""
            if (basePath.isNotEmpty()) "$basePath/wireone" else ""
        }
    }

    if (rootPath.isNotEmpty()) {
        NSFileManager.defaultManager.createDirectoryAtPath(
            path = rootPath,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )
    }

    return rootPath
}
