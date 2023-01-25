/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.di

import com.wire.android.BuildConfig
import com.wire.kalium.logic.configuration.server.ServerConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthServerConfigProvider @Inject constructor() {
    //todo check with soft logout
    private val serverConfigDefaultLinks = if (BuildConfig.IS_STAGING) ServerConfig.STAGING else ServerConfig.PRODUCTION
    private val _authServer: MutableStateFlow<ServerConfig.Links> = MutableStateFlow(serverConfigDefaultLinks)
    val authServer: StateFlow<ServerConfig.Links> = _authServer

    fun updateAuthServer(serverLinks: ServerConfig.Links) {
        _authServer.value = serverLinks
    }

    fun updateAuthServer(serverConfig: ServerConfig) {
        _authServer.value = serverConfig.links
    }
}
