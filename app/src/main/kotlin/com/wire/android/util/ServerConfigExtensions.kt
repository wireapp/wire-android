/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.util

import com.wire.kalium.logic.configuration.server.ServerConfig

/**
 * Checks if a [ServerConfig] is a valid environment for analytics, returning true in case can be enabled.
 */
fun ServerConfig.isHostValidForAnalytics(): Boolean {
    return this.links.isHostValidForAnalytics()
}

/**
 * Checks if a [ServerConfig.Links] is a valid environment for analytics, returning true in case can be enabled.
 */
fun ServerConfig.Links.isHostValidForAnalytics(): Boolean {
    return this.api == ServerConfig.PRODUCTION.api
            || this.api == ServerConfig.STAGING.api
}
