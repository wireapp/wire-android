/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.util.lifecycle

import com.wire.android.AppJsonStyledLogger
import com.wire.kalium.logger.KaliumLogLevel
import com.wire.kalium.logger.KaliumLogger

internal enum class AppSyncTelemetryEvent {
    APP_SYNC_VISIBILITY_CHANGED,
    APP_SYNC_REQUEST_STARTED,
    APP_SYNC_REQUEST_RELEASED,
    APP_SYNC_WAIT_STARTED,
    APP_SYNC_WAIT_COMPLETED,
}

internal enum class AppSyncTelemetryTrigger {
    APP_FOREGROUND,
    PUSH_NOTIFICATION,
}

internal enum class AppSyncTelemetryOutcome {
    SUCCESS,
    FAILURE,
}

internal fun KaliumLogger.logAppSyncTelemetry(
    event: AppSyncTelemetryEvent,
    data: Map<String, Any?> = emptyMap(),
    level: KaliumLogLevel = KaliumLogLevel.INFO,
) {
    AppJsonStyledLogger.log(
        level = level,
        leadingMessage = "Sync telemetry",
        jsonStringKeyValues = buildMap {
            put("schemaVersion", 1)
            put("event", event.name)
            put("component", "APP_LIFECYCLE")
            data.forEach { (key, value) ->
                value?.let { put(key, it) }
            }
        },
        logger = this,
    )
}
