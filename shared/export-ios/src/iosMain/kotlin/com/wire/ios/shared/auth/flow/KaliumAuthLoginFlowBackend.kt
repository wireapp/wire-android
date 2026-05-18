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
package com.wire.shared.auth.flow

import com.wire.shared.auth.email.LoginEmailGateway
import com.wire.shared.auth.email.LoginEmailGatewayResult
import com.wire.shared.auth.email.LoginEmailError
import com.wire.shared.auth.newlogin.NewLoginIdentifierBackend
import com.wire.shared.auth.newlogin.NewLoginIdentifierBackendResult
import com.wire.shared.auth.newlogin.NewLoginIdentifierDialogError
import dev.zacsweers.metro.Inject

@Inject
class KaliumAuthLoginFlowBackend(
    private val identifierBackend: NewLoginIdentifierBackend,
    private val emailGateway: LoginEmailGateway,
) : AuthLoginFlowBackend {
    override suspend fun resolveIdentifier(identifier: String): AuthLoginFlowIdentifierResult =
        identifierBackend.resolveEmail(identifier).toFlowIdentifierResult()

    override suspend fun initiateSso(ssoCode: String): AuthLoginFlowIdentifierResult =
        identifierBackend.initiateSso(ssoCode).toFlowIdentifierResult()

    override suspend fun loginWithEmail(
        identifier: String,
        password: String,
        secondFactorCode: String?,
        usernameAllowed: Boolean,
    ): AuthLoginFlowLoginResult =
        emailGateway.login(
            userIdentifier = identifier,
            password = password,
            secondFactorVerificationCode = secondFactorCode,
            usernameAllowed = usernameAllowed,
        ).toFlowLoginResult()
}

private fun NewLoginIdentifierBackendResult.toFlowIdentifierResult(): AuthLoginFlowIdentifierResult =
    when (this) {
        is NewLoginIdentifierBackendResult.OpenEmailPassword ->
            AuthLoginFlowIdentifierResult.EmailCredentialsRequired(userIdentifier)

        is NewLoginIdentifierBackendResult.OpenSso ->
            AuthLoginFlowIdentifierResult.OpenSso(
                url = url,
                userIdentifier = config.userIdentifier,
            )

        is NewLoginIdentifierBackendResult.Error ->
            AuthLoginFlowIdentifierResult.Failure(error.toFlowError())

        is NewLoginIdentifierBackendResult.EnterpriseLoginNotSupported,
        is NewLoginIdentifierBackendResult.OpenCustomConfig ->
            AuthLoginFlowIdentifierResult.Failure(AuthLoginFlowError.Generic())
    }

private fun LoginEmailGatewayResult.toFlowLoginResult(): AuthLoginFlowLoginResult =
    when (this) {
        is LoginEmailGatewayResult.Success ->
            AuthLoginFlowLoginResult.Success(
                initialSyncCompleted = initialSyncCompleted,
                isE2EIRequired = isE2EIRequired,
                payload = payload,
            )

        is LoginEmailGatewayResult.SecondFactorRequired ->
            AuthLoginFlowLoginResult.SecondFactorRequired(
                email = email,
                isCurrentCodeInvalid = isCurrentCodeInvalid,
            )

        LoginEmailGatewayResult.RemoveDeviceNeeded ->
            AuthLoginFlowLoginResult.RemoveDeviceNeeded

        is LoginEmailGatewayResult.Failure ->
            AuthLoginFlowLoginResult.Failure(error.toFlowError())
    }

private fun NewLoginIdentifierDialogError.toFlowError(): AuthLoginFlowError =
    when (this) {
        NewLoginIdentifierDialogError.InvalidSSOCode,
        NewLoginIdentifierDialogError.InvalidSSOCookie ->
            AuthLoginFlowError.InvalidIdentifier

        else ->
            AuthLoginFlowError.Generic()
    }

private fun LoginEmailError.toFlowError(): AuthLoginFlowError =
    when (this) {
        LoginEmailError.InvalidCredentials ->
            AuthLoginFlowError.InvalidCredentials

        LoginEmailError.TooManyDevices ->
            AuthLoginFlowError.TooManyDevices

        else ->
            AuthLoginFlowError.Generic()
    }
