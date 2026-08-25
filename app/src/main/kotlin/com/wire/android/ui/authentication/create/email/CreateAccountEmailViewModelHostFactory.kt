/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.authentication.create.email

import com.wire.android.di.KaliumCoreLogic
import com.wire.android.navigation.routes.auth.AuthenticationServerLinks
import com.wire.android.navigation.routes.auth.CreateAccountRouteFlowType
import com.wire.android.navigation.routes.auth.toLegacy
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.auth.ValidateEmailUseCase
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.register.RequestActivationCodeResult
import dev.zacsweers.metro.Inject

internal class KaliumCreateAccountEmailGateway(
    private val validateEmail: ValidateEmailUseCase,
    private val coreLogic: CoreLogic,
) : CreateAccountEmailGateway<ServerConfig.Links, CoreFailure> {
    override fun isEmailValid(email: String): Boolean = validateEmail(email)

    override suspend fun requestActivationCode(
        serverConfig: ServerConfig.Links,
        email: () -> String,
    ): ActivationCodeResult<CoreFailure> {
        val authScope = when (val result = coreLogic.versionedAuthenticationScope(serverConfig)(null)) {
            is AutoVersionAuthScopeUseCase.Result.Success -> result.authenticationScope
            is AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion,
            is AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion,
            is AutoVersionAuthScopeUseCase.Result.Failure.Generic -> return ActivationCodeResult.AuthScopeUnavailable
        }
        return when (val result = authScope.registerScope.requestActivationCode(email())) {
            RequestActivationCodeResult.Success -> ActivationCodeResult.Sent
            RequestActivationCodeResult.Failure.AlreadyInUse -> ActivationCodeResult.AlreadyInUse
            RequestActivationCodeResult.Failure.BlacklistedEmail -> ActivationCodeResult.Blacklisted
            RequestActivationCodeResult.Failure.DomainBlocked -> ActivationCodeResult.DomainBlocked
            RequestActivationCodeResult.Failure.InvalidEmail -> ActivationCodeResult.InvalidEmail
            is RequestActivationCodeResult.Failure.Generic -> ActivationCodeResult.Generic(result.failure)
        }
    }
}

class CreateAccountEmailViewModelHostFactory @Inject constructor(
    validateEmail: ValidateEmailUseCase,
    @KaliumCoreLogic coreLogic: CoreLogic,
    private val defaultServerConfig: ServerConfig.Links,
) {
    private val gateway = KaliumCreateAccountEmailGateway(validateEmail, coreLogic)

    fun create(
        type: CreateAccountRouteFlowType,
        customServerConfig: AuthenticationServerLinks?,
    ): CreateAccountEmailViewModel<CreateAccountRouteFlowType, ServerConfig.Links, CoreFailure> = CreateAccountEmailViewModel(
        flowType = type,
        customServerConfig = customServerConfig?.toLegacy(),
        defaultServerConfig = defaultServerConfig,
        tosUrlFor = { it.tos },
        gateway = gateway,
    )
}
