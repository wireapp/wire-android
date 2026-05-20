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
package com.wire.android.ui.authentication.login.sso

import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.di.DefaultWebSocketEnabledByDefault
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.LoginSavedInputStore
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.ValidateEmailUseCase
import dev.zacsweers.metro.Inject

@Inject
@Suppress("LongParameterList")
class LoginSSOViewModelFactory(
    private val savedInputStore: LoginSavedInputStore,
    private val addAuthenticatedUser: AddAuthenticatedUserUseCase,
    private val validateEmailUseCase: ValidateEmailUseCase,
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val clientScopeProviderFactory: ClientScopeProvider.Factory,
    private val userDataStoreProvider: UserDataStoreProvider,
    private val serverConfig: ServerConfig.Links,
    @DefaultWebSocketEnabledByDefault private val defaultWebSocketEnabledByDefault: Boolean,
    private val sessionExceptionClassifier: LoginSSOSessionExceptionClassifier,
    private val dispatchers: DispatcherProvider,
) {
    fun create(args: LoginNavArgs): LoginSSOViewModel = LoginSSOViewModel(
        loginNavArgs = args,
        savedInputStore = savedInputStore,
        addAuthenticatedUser = addAuthenticatedUser,
        validateEmailUseCase = validateEmailUseCase,
        coreLogic = coreLogic,
        clientScopeProviderFactory = clientScopeProviderFactory,
        userDataStoreProvider = userDataStoreProvider,
        serverConfig = serverConfig,
        defaultWebSocketEnabledByDefault = defaultWebSocketEnabledByDefault,
        sessionExceptionClassifier = sessionExceptionClassifier,
        dispatchers = dispatchers,
    )
}
