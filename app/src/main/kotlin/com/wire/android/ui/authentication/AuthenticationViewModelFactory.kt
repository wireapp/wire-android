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
package com.wire.android.ui.authentication

import androidx.lifecycle.SavedStateHandle
import com.wire.android.analytics.FinalizeRegistrationAnalyticsMetadataUseCase
import com.wire.android.analytics.RegistrationAnalyticsManagerUseCase
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.datastore.UserDataStore
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.di.DefaultWebSocketEnabledByDefault
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.ui.authentication.create.code.CreateAccountCodeViewModel
import com.wire.android.ui.authentication.create.details.CreateAccountDetailsViewModel
import com.wire.android.ui.authentication.create.email.CreateAccountEmailViewModel
import com.wire.android.ui.authentication.create.overview.CreateAccountOverviewViewModel
import com.wire.android.ui.authentication.create.summary.CreateAccountSummaryViewModel
import com.wire.android.ui.authentication.create.username.CreateAccountUsernameViewModel
import com.wire.android.ui.authentication.devices.common.ClearSessionViewModel
import com.wire.android.ui.authentication.devices.register.RegisterDeviceViewModel
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceViewModel
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.LoginSavedInputStore
import com.wire.android.ui.authentication.login.LoginViewModelExtension
import com.wire.android.ui.authentication.login.SavedStateLoginSavedInputStore
import com.wire.android.ui.authentication.login.email.LoginEmailViewModel
import com.wire.android.ui.authentication.login.sso.LoginSSOViewModel
import com.wire.android.ui.authentication.login.sso.LoginSSOViewModelExtension
import com.wire.android.ui.authentication.welcome.WelcomeViewModel
import com.wire.android.ui.newauthentication.login.NewLoginViewModel
import com.wire.android.ui.newauthentication.login.ValidateEmailOrSSOCodeUseCase
import com.wire.android.ui.registration.code.CreateAccountVerificationCodeViewModel
import com.wire.android.ui.registration.details.CreateAccountDataDetailViewModel
import com.wire.android.ui.registration.selector.CreateAccountSelectorViewModel
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.CountdownTimer
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.LogoutUseCase
import com.wire.kalium.logic.feature.auth.ValidateEmailUseCase
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.auth.ValidateUserHandleUseCase
import com.wire.kalium.logic.feature.auth.verification.RequestSecondFactorVerificationCodeUseCase
import com.wire.kalium.logic.feature.client.DeleteClientUseCase
import com.wire.kalium.logic.feature.client.FetchSelfClientsFromRemoteUseCase
import com.wire.kalium.logic.feature.client.GetOrRegisterClientUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.feature.session.DeleteSessionUseCase
import com.wire.kalium.logic.feature.session.DoesValidNomadAccountExistUseCase
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import com.wire.kalium.logic.feature.user.SetUserHandleUseCase
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.Provider

@Suppress("LongParameterList", "TooManyFunctions")
class AuthenticationViewModelFactory @Inject constructor(
    private val globalDataStore: Provider<GlobalDataStore>,
    private val userDataStore: Provider<UserDataStore>,
    private val userDataStoreProvider: Provider<UserDataStoreProvider>,
    @KaliumCoreLogic private val coreLogic: Provider<CoreLogic>,
    private val defaultServerConfig: Provider<ServerConfig.Links>,
    @DefaultWebSocketEnabledByDefault private val defaultWebSocketEnabledByDefault: Provider<Boolean>,
    @Named("ssoCodeConfig") private val defaultSSOCodeConfig: Provider<String>,
    private val addAuthenticatedUser: Provider<AddAuthenticatedUserUseCase>,
    private val clientScopeProviderFactory: Provider<ClientScopeProvider.Factory>,
    private val dispatchers: Provider<DispatcherProvider>,
    private val validateEmailOrSSOCode: Provider<ValidateEmailOrSSOCodeUseCase>,
    private val validateEmail: Provider<ValidateEmailUseCase>,
    private val validatePassword: Provider<ValidatePasswordUseCase>,
    private val validateUserHandle: Provider<ValidateUserHandleUseCase>,
    private val setUserHandle: Provider<SetUserHandleUseCase>,
    private val finalizeRegistrationAnalyticsMetadata: Provider<FinalizeRegistrationAnalyticsMetadataUseCase>,
    private val registrationAnalyticsManager: Provider<RegistrationAnalyticsManagerUseCase>,
    private val getSessions: Provider<GetSessionsUseCase>,
    private val doesValidNomadAccountExist: Provider<DoesValidNomadAccountExistUseCase>,
    private val currentSession: Provider<CurrentSessionUseCase>,
    private val deleteSession: Provider<DeleteSessionUseCase>,
    private val switchAccount: Provider<AccountSwitchUseCase>,
    private val logout: Provider<LogoutUseCase>,
    private val getOrRegisterClient: Provider<GetOrRegisterClientUseCase>,
    private val isPasswordRequired: Provider<IsPasswordRequiredUseCase>,
    private val getSelfUser: Provider<GetSelfUserUseCase>,
    private val requestSecondFactorVerificationCode: Provider<RequestSecondFactorVerificationCodeUseCase>,
    private val countdownTimer: Provider<CountdownTimer>,
    private val fetchSelfClientsFromRemote: Provider<FetchSelfClientsFromRemoteUseCase>,
    private val deleteClient: Provider<DeleteClientUseCase>,
) {
    fun welcomeViewModel(savedStateHandle: SavedStateHandle) = WelcomeViewModel(
        savedStateHandle = savedStateHandle,
        getSessions = getSessions(),
        doesValidNomadAccountExist = doesValidNomadAccountExist(),
        defaultServerConfig = defaultServerConfig(),
    )

    fun newLoginViewModel(savedStateHandle: SavedStateHandle) = NewLoginViewModel(
        validateEmailOrSSOCode = validateEmailOrSSOCode(),
        coreLogic = coreLogic(),
        savedStateHandle = savedStateHandle,
        clientScopeProviderFactory = clientScopeProviderFactory(),
        userDataStoreProvider = userDataStoreProvider(),
        loginExtension = LoginViewModelExtension(clientScopeProviderFactory(), userDataStoreProvider()),
        ssoExtension = LoginSSOViewModelExtension(
            addAuthenticatedUser = addAuthenticatedUser(),
            coreLogic = coreLogic(),
            defaultWebSocketEnabledByDefault = defaultWebSocketEnabledByDefault(),
        ),
        dispatchers = dispatchers(),
        defaultServerConfig = defaultServerConfig(),
        defaultSSOCodeConfig = defaultSSOCodeConfig(),
    )

    fun loginEmailViewModel(loginNavArgs: LoginNavArgs, savedStateHandle: SavedStateHandle) = LoginEmailViewModel(
        loginNavArgs = loginNavArgs,
        addAuthenticatedUser = addAuthenticatedUser(),
        clientScopeProviderFactory = clientScopeProviderFactory(),
        savedInputStore = loginSavedInputStore(savedStateHandle),
        userDataStoreProvider = userDataStoreProvider(),
        coreLogic = coreLogic(),
        resendCodeTimer = countdownTimer(),
        dispatchers = dispatchers(),
        defaultServerConfig = defaultServerConfig(),
        defaultWebSocketEnabledByDefault = defaultWebSocketEnabledByDefault(),
    )

    fun loginSSOViewModel(loginNavArgs: LoginNavArgs, savedStateHandle: SavedStateHandle) = LoginSSOViewModel(
        loginNavArgs = loginNavArgs,
        savedInputStore = loginSavedInputStore(savedStateHandle),
        addAuthenticatedUser = addAuthenticatedUser(),
        validateEmailUseCase = validateEmail(),
        coreLogic = coreLogic(),
        clientScopeProviderFactory = clientScopeProviderFactory(),
        userDataStoreProvider = userDataStoreProvider(),
        serverConfig = defaultServerConfig(),
        ssoExtension = LoginSSOViewModelExtension(
            addAuthenticatedUser = addAuthenticatedUser(),
            coreLogic = coreLogic(),
            defaultWebSocketEnabledByDefault = defaultWebSocketEnabledByDefault(),
        ),
        dispatchers = dispatchers(),
    )

    fun registerDeviceViewModel() = RegisterDeviceViewModel(
        registerClientUseCase = getOrRegisterClient(),
        isPasswordRequired = isPasswordRequired(),
        userDataStore = userDataStore(),
        getSelfUser = getSelfUser(),
        requestSecondFactorVerificationCodeUseCase = requestSecondFactorVerificationCode(),
        resendCodeTimer = countdownTimer(),
    )

    fun removeDeviceViewModel() = RemoveDeviceViewModel(
        fetchSelfClientsFromRemote = fetchSelfClientsFromRemote(),
        deleteClientUseCase = deleteClient(),
        registerClientUseCase = getOrRegisterClient(),
        isPasswordRequired = isPasswordRequired(),
        userDataStore = userDataStore(),
        getSelfUser = getSelfUser(),
        requestSecondFactorVerificationCodeUseCase = requestSecondFactorVerificationCode(),
    )

    fun clearSessionViewModel() = ClearSessionViewModel(
        currentSession = currentSession(),
        deleteSession = deleteSession(),
        switchAccount = switchAccount(),
        logout = logout(),
    )

    fun createAccountUsernameViewModel() = CreateAccountUsernameViewModel(
        validateUserHandleUseCase = validateUserHandle(),
        setUserHandleUseCase = setUserHandle(),
        finalizeRegistrationAnalyticsMetadata = finalizeRegistrationAnalyticsMetadata(),
        registrationAnalyticsManager = registrationAnalyticsManager(),
    )

    fun createAccountOverviewViewModel(savedStateHandle: SavedStateHandle) = CreateAccountOverviewViewModel(
        savedStateHandle = savedStateHandle,
        defaultServerConfig = defaultServerConfig(),
    )

    fun createAccountEmailViewModel(savedStateHandle: SavedStateHandle) = CreateAccountEmailViewModel(
        savedStateHandle = savedStateHandle,
        validateEmail = validateEmail(),
        coreLogic = coreLogic(),
        defaultServerConfig = defaultServerConfig(),
    )

    fun createAccountDetailsViewModel(savedStateHandle: SavedStateHandle) = CreateAccountDetailsViewModel(
        savedStateHandle = savedStateHandle,
        validatePasswordUseCase = validatePassword(),
        defaultServerConfig = defaultServerConfig(),
    )

    fun createAccountCodeViewModel(savedStateHandle: SavedStateHandle) = CreateAccountCodeViewModel(
        savedStateHandle = savedStateHandle,
        coreLogic = coreLogic(),
        addAuthenticatedUser = addAuthenticatedUser(),
        clientScopeProviderFactory = clientScopeProviderFactory(),
        defaultServerConfig = defaultServerConfig(),
        defaultWebSocketEnabledByDefault = defaultWebSocketEnabledByDefault(),
    )

    fun createAccountSummaryViewModel(savedStateHandle: SavedStateHandle) =
        CreateAccountSummaryViewModel(savedStateHandle = savedStateHandle)

    fun createAccountSelectorViewModel(savedStateHandle: SavedStateHandle) = CreateAccountSelectorViewModel(
        globalDataStore = globalDataStore(),
        savedStateHandle = savedStateHandle,
        defaultServerConfig = defaultServerConfig(),
    )

    fun createAccountDataDetailViewModel(savedStateHandle: SavedStateHandle) = CreateAccountDataDetailViewModel(
        savedStateHandle = savedStateHandle,
        validatePassword = validatePassword(),
        validateEmail = validateEmail(),
        globalDataStore = globalDataStore(),
        registrationAnalyticsManager = registrationAnalyticsManager(),
        coreLogic = coreLogic(),
        defaultServerConfig = defaultServerConfig(),
    )

    fun createAccountVerificationCodeViewModel(savedStateHandle: SavedStateHandle) = CreateAccountVerificationCodeViewModel(
        savedStateHandle = savedStateHandle,
        coreLogic = coreLogic(),
        addAuthenticatedUser = addAuthenticatedUser(),
        registrationAnalyticsManager = registrationAnalyticsManager(),
        clientScopeProviderFactory = clientScopeProviderFactory(),
        defaultServerConfig = defaultServerConfig(),
        defaultWebSocketEnabledByDefault = defaultWebSocketEnabledByDefault(),
    )

    private fun loginSavedInputStore(savedStateHandle: SavedStateHandle): LoginSavedInputStore =
        SavedStateLoginSavedInputStore(savedStateHandle)
}
