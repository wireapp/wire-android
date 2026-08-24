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

package com.wire.android.ui.authentication.devices.register

import com.wire.android.BuildConfig
import com.wire.android.datastore.UserDataStore
import com.wire.android.util.ui.CountdownTimer
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.logic.data.auth.verification.VerifiableAction
import com.wire.kalium.logic.feature.auth.verification.RequestSecondFactorVerificationCodeUseCase
import com.wire.kalium.logic.feature.client.GetOrRegisterClientUseCase
import com.wire.kalium.logic.feature.client.RegisterClientParam
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import com.wire.navigation.WireSessionId
import kotlinx.coroutines.flow.first

internal class KaliumRegisterDeviceGateway(
    private val registerClient: GetOrRegisterClientUseCase,
    private val isPasswordRequired: IsPasswordRequiredUseCase,
    private val userDataStore: UserDataStore,
    private val getSelfUser: GetSelfUserUseCase,
    private val requestSecondFactorVerificationCode: RequestSecondFactorVerificationCodeUseCase,
    private val buildFlags: RegisterDeviceBuildFlags = RegisterDeviceBuildFlags.current(),
) : RegisterDeviceGateway<WireSessionId> {

    override suspend fun passwordRequirement(): PasswordRequirement = when (val result = isPasswordRequired()) {
        is IsPasswordRequiredUseCase.Result.Success -> if (result.value) {
            PasswordRequirement.Required
        } else {
            PasswordRequirement.NotRequired
        }

        is IsPasswordRequiredUseCase.Result.Failure ->
            PasswordRequirement.Failure(result.cause.toAuthenticationFailure())
    }

    override suspend fun registerClient(request: RegisterDeviceRequest): RegisterDeviceResult<WireSessionId> =
        when (
            val result = registerClient(
                RegisterClientParam(
                    password = request.password,
                    secondFactorVerificationCode = request.verificationCode,
                    capabilities = null,
                    modelPostfix = buildFlags.modelPostfix,
                )
            )
        ) {
            is RegisterClientResult.Success -> RegisterDeviceResult.Success(
                initialSyncCompleted = userDataStore.initialSyncCompleted.first(),
                isE2EIRequired = false,
            )

            is RegisterClientResult.E2EICertificateRequired -> RegisterDeviceResult.Success(
                initialSyncCompleted = userDataStore.initialSyncCompleted.first(),
                isE2EIRequired = true,
                e2eiSessionId = WireSessionId(result.userId.value, result.userId.domain),
            )

            RegisterClientResult.Failure.TooManyClients -> RegisterDeviceResult.TooManyDevices
            RegisterClientResult.Failure.InvalidCredentials.Missing2FA -> RegisterDeviceResult.MissingSecondFactor
            RegisterClientResult.Failure.InvalidCredentials.Invalid2FA -> RegisterDeviceResult.InvalidSecondFactor
            is RegisterClientResult.Failure.InvalidCredentials -> RegisterDeviceResult.InvalidCredentials
            RegisterClientResult.Failure.PasswordAuthRequired -> RegisterDeviceResult.PasswordRequired
            is RegisterClientResult.Failure.Generic ->
                RegisterDeviceResult.Failure(result.genericFailure.toAuthenticationFailure())
        }

    override suspend fun requestVerificationCode(): RequestVerificationCodeResult {
        val email = getSelfUser()?.email ?: return RequestVerificationCodeResult.MissingEmail
        return when (
            val result = requestSecondFactorVerificationCode(
                email = email,
                verifiableAction = VerifiableAction.LOGIN_OR_CLIENT_REGISTRATION,
            )
        ) {
            RequestSecondFactorVerificationCodeUseCase.Result.Success -> RequestVerificationCodeResult.Sent(email)
            RequestSecondFactorVerificationCodeUseCase.Result.Failure.TooManyRequests ->
                RequestVerificationCodeResult.TooManyRequests(email)

            is RequestSecondFactorVerificationCodeUseCase.Result.Failure.Generic ->
                RequestVerificationCodeResult.Failure(result.cause.toAuthenticationFailure())

            else -> RequestVerificationCodeResult.Failure(AuthenticationFailure.Unknown)
        }
    }
}

internal data class RegisterDeviceBuildFlags(
    val privateBuild: Boolean,
    val flavor: String,
    val buildType: String,
) {
    val modelPostfix: String?
        get() = if (privateBuild) " [${flavor}_${buildType}]" else null

    companion object {
        fun current() = RegisterDeviceBuildFlags(
            privateBuild = BuildConfig.PRIVATE_BUILD,
            flavor = BuildConfig.FLAVOR,
            buildType = BuildConfig.BUILD_TYPE,
        )
    }
}

internal class AndroidRegisterDeviceResendTimer(
    private val countdownTimer: CountdownTimer,
) : RegisterDeviceResendTimer {
    override suspend fun start(seconds: Long, onUpdate: (String) -> Unit, onFinish: () -> Unit) {
        countdownTimer.start(seconds, onUpdate, onFinish)
    }
}

internal fun CoreFailure.toAuthenticationFailure(): AuthenticationFailure = when (this) {
    is NetworkFailure.NoNetworkConnection -> AuthenticationFailure.NoNetwork
    is NetworkFailure.ServerMiscommunication -> AuthenticationFailure.ServerMiscommunication
    else -> AuthenticationFailure.Unknown
}
