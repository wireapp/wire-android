/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.ui.authentication.create.code

import com.wire.android.di.ClientScopeProvider
import com.wire.android.di.DefaultWebSocketEnabledByDefault
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.navigation.routes.auth.AuthenticationServerLinks
import com.wire.android.navigation.routes.auth.CreateAccountRegistrationInfo
import com.wire.android.navigation.routes.auth.CreateAccountRouteFlowType
import com.wire.android.navigation.routes.auth.toLegacy
import com.wire.android.ui.authentication.create.common.createAccountFlowPolicy
import com.wire.android.util.ui.CountdownTimer
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import dev.zacsweers.metro.Inject

class CreateAccountCodeViewModelHostFactory @Inject constructor(
    @KaliumCoreLogic coreLogic: CoreLogic,
    addAuthenticatedUser: AddAuthenticatedUserUseCase,
    clientScopeProviderFactory: ClientScopeProvider.Factory,
    private val defaultServerConfig: ServerConfig.Links,
    @DefaultWebSocketEnabledByDefault defaultWebSocketEnabledByDefault: Boolean,
) {
    private val gateway = KaliumCreateAccountCodeGateway(
        coreLogic,
        addAuthenticatedUser,
        clientScopeProviderFactory,
        defaultWebSocketEnabledByDefault,
    )

    fun create(
        type: CreateAccountRouteFlowType,
        registrationInfo: CreateAccountRegistrationInfo,
        customServerConfig: AuthenticationServerLinks?,
    ): AppCreateAccountCodeViewModel = CreateAccountCodeViewModel(
        input = registrationInfo.toInput(type, customServerConfig),
        defaultServerConfig = defaultServerConfig,
        gateway = gateway,
        resendCodeTimer = AndroidCreateAccountCodeResendTimer(CountdownTimer()),
    )
}

private fun CreateAccountRegistrationInfo.toInput(
    type: CreateAccountRouteFlowType,
    customServerConfig: AuthenticationServerLinks?,
) = CreateAccountCodeInput(
    flowType = type,
    customServerConfig = customServerConfig?.toLegacy(),
    email = email,
    firstName = firstName,
    lastName = lastName,
    password = password,
    teamName = teamName,
    isTeam = type.createAccountFlowPolicy().isTeam,
)
