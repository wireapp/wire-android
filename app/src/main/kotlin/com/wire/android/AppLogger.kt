/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android

import com.wire.kalium.logger.KaliumLogLevel
import com.wire.kalium.logger.KaliumLogger

private var appLoggerConfig = KaliumLogger.Config.disabled()
// App wide global logger, carefully initialized when our application is "onCreate"
internal var appLogger = KaliumLogger.disabled()
object AppLogger {
    fun init(config: KaliumLogger.Config) {
        appLoggerConfig = config
        appLogger = KaliumLogger(config = config, tag = "WireAppLogger")
    }
    fun setLogLevel(level: KaliumLogLevel) {
        appLoggerConfig.setLogLevel(level)
    }
}
