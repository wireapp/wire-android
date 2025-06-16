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
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.auth.LoginContext
import dagger.Lazy
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Selector for login type, used to determine if the new login flow should be used. If the [LoginContext] for the given
 * [ServerConfig.Links] is [LoginContext.EnterpriseLogin] then the new login flow can be used, otherwise fallback to the old login flow.
 */
@Singleton
class LoginTypeSelector @Inject constructor(
    @KaliumCoreLogic private val coreLogic: Lazy<CoreLogic>,
    @Named("useNewLoginForDefaultBackend") private val useNewLoginForDefaultBackend: Boolean,
) {

    /**
     * Observe the [LoginContext] for the given [ServerConfig.Links].
     */
    private suspend fun loginContextFlow(serverLinks: ServerConfig.Links) =
        coreLogic.get().getGlobalScope().observeLoginContext(serverLinks)

    /**
     *  Determine if the new login flow can be used for the given [ServerConfig.Links].
     */
    suspend fun canUseNewLogin(serverLinks: ServerConfig.Links?) = when {
        // if the server links are provided, get the login context for the given server links and check if it's enterprise login
        serverLinks != null -> loginContextFlow(serverLinks).first() == LoginContext.EnterpriseLogin
        // otherwise, use the function for the default server config links to determine if the new login flow can be used
        else -> canUseNewLogin()
    }

    /**
     * Determine if the new login flow can be used for the default [ServerConfig.Links] - [DefaultServerConfig].
     */
    fun canUseNewLogin() = useNewLoginForDefaultBackend
}
