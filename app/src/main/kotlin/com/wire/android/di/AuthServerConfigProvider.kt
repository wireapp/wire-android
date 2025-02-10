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

package com.wire.android.di

import com.wire.android.BuildConfig
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.configuration.server.ServerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthServerConfigProvider @Inject constructor(
    dispatcherProvider: DispatcherProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.default())
    private val _authServerSuspending: MutableSharedFlow<ServerConfig.Links> = MutableSharedFlow(1, 0, BufferOverflow.SUSPEND)
    // suspends until all subscribers handle the new value so that data is always in sync before doing anything else
    val authServerSuspending: SharedFlow<ServerConfig.Links> = _authServerSuspending
    // non suspending state version for easier usage when there is no need to keep data in sync
    val authServer: StateFlow<ServerConfig.Links> = _authServerSuspending.stateIn(scope, SharingStarted.Eagerly, DefaultServerConfig)

    suspend fun updateAuthServer(serverLinks: ServerConfig.Links) {
        _authServerSuspending.emit(serverLinks)
    }
}

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
