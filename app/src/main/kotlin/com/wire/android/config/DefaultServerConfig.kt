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
package com.wire.android.config

import com.wire.android.BuildConfig
import com.wire.kalium.logic.configuration.server.ServerConfig

val DefaultServerConfig = ServerConfig.Links(
    api = BuildConfig.DEFAULT_BACKEND_URL_BASE_API,
    accounts = BuildConfig.DEFAULT_BACKEND_URL_ACCOUNTS,
    webSocket = BuildConfig.DEFAULT_BACKEND_URL_BASE_WEBSOCKET,
    teams = BuildConfig.DEFAULT_BACKEND_URL_TEAM_MANAGEMENT,
    blackList = BuildConfig.DEFAULT_BACKEND_URL_BLACKLIST,
    website = BuildConfig.DEFAULT_BACKEND_URL_WEBSITE,
    title = BuildConfig.DEFAULT_BACKEND_TITLE,
    isOnPremises = false,
    apiProxy = null
)

fun ServerConfig.Links?.orDefault() = this ?: DefaultServerConfig
