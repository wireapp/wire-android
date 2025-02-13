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

import com.wire.android.config.DefaultServerConfig
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.auth.LoginContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoginTypeSelector @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    @KaliumCoreLogic private val coreLogic: CoreLogic,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.default())
    private val loginContextForDefaultServerConfigStateFlow: StateFlow<LoginContext> = runBlocking {
        // it needs to be initialised before navigation is setup
        loginContextFlow(DefaultServerConfig).stateIn(scope, SharingStarted.Eagerly, loginContextFlow(DefaultServerConfig).first())
    }
    private suspend fun loginContextFlow(serverLinks: ServerConfig.Links) = coreLogic.getGlobalScope().observeLoginContext(serverLinks)

    suspend fun canUseNewLogin(serverLinks: ServerConfig.Links?) = when {
        serverLinks != null -> loginContextFlow(serverLinks).first() == LoginContext.EnterpriseLogin
        else -> canUseNewLogin()
    }

    fun canUseNewLogin() = loginContextForDefaultServerConfigStateFlow.value == LoginContext.EnterpriseLogin
}
