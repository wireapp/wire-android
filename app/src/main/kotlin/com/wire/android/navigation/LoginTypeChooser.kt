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
package com.wire.android.navigation

import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoginTypeChooser @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    private val authServerConfigProvider: AuthServerConfigProvider,
    @KaliumCoreLogic private val coreLogic: CoreLogic,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.default())
    private lateinit var canUseNewLoginStateFlow: StateFlow<Boolean>

    private fun canUseNewLoginFlow(serverLinks: ServerConfig.Links): Flow<Boolean> = // TODO: replace with proper use case
        flow {
            delay(2000)
            emit(serverLinks.api.contains("prod"))
        }

    // needs to be called before navigating or setting up the initial screen
    suspend fun initLoginTypeChooser() {
        if (::canUseNewLoginStateFlow.isInitialized.not()) {
            canUseNewLoginStateFlow = authServerConfigProvider.authServerSuspending
                .flatMapLatest { serverLinks ->
                    canUseNewLoginFlow(serverLinks)
                }
                .stateIn(scope, SharingStarted.Eagerly, canUseNewLoginFlow(authServerConfigProvider.authServer.value).first())
        }
    }

    // when using custom server config make sure that `AuthServerConfigProvider.updateAuthServer` is executed before calling this one
    fun canUseNewLogin(): Boolean = canUseNewLoginStateFlow.value
}
