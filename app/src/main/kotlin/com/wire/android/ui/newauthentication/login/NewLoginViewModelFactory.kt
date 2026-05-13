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
package com.wire.android.ui.newauthentication.login

import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.di.DefaultWebSocketEnabledByDefault
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.LoginSavedInputStore
import com.wire.android.ui.authentication.login.LoginViewModelExtension
import com.wire.android.ui.authentication.login.sso.LoginSSOViewModelExtension
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named

@Inject
@Suppress("LongParameterList")
class NewLoginViewModelFactory(
    private val validateEmailOrSSOCode: ValidateEmailOrSSOCodeUseCase,
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val addAuthenticatedUser: AddAuthenticatedUserUseCase,
    private val clientScopeProviderFactory: ClientScopeProvider.Factory,
    private val userDataStoreProvider: UserDataStoreProvider,
    private val dispatchers: DispatcherProvider,
    private val defaultServerConfig: ServerConfig.Links,
    @Named("ssoCodeConfig") private val defaultSSOCodeConfig: String,
    @DefaultWebSocketEnabledByDefault private val defaultWebSocketEnabledByDefault: Boolean,
    private val recoverableLogoutExceptionDetector: NewLoginRecoverableLogoutExceptionDetector,
) {
    fun create(
        args: LoginNavArgs,
        savedInputStore: LoginSavedInputStore,
    ): NewLoginViewModel = NewLoginViewModel(
        loginNavArgs = args,
        validateEmailOrSSOCode = validateEmailOrSSOCode,
        coreLogic = coreLogic,
        savedInputStore = savedInputStore,
        clientScopeProviderFactory = clientScopeProviderFactory,
        userDataStoreProvider = userDataStoreProvider,
        loginExtension = LoginViewModelExtension(clientScopeProviderFactory, userDataStoreProvider),
        ssoExtension = LoginSSOViewModelExtension(addAuthenticatedUser, coreLogic, defaultWebSocketEnabledByDefault),
        dispatchers = dispatchers,
        defaultServerConfig = defaultServerConfig,
        defaultSSOCodeConfig = defaultSSOCodeConfig,
        recoverableLogoutExceptionDetector = recoverableLogoutExceptionDetector,
    )
}
