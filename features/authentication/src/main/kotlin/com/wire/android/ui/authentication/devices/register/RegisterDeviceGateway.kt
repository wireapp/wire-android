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

interface RegisterDeviceGateway<SessionT> {
    suspend fun passwordRequirement(): PasswordRequirement

    suspend fun registerClient(request: RegisterDeviceRequest): RegisterDeviceResult<SessionT>

    suspend fun requestVerificationCode(): RequestVerificationCodeResult
}

data class RegisterDeviceRequest(
    val password: String?,
    val verificationCode: String?,
)

sealed interface PasswordRequirement {
    data object Required : PasswordRequirement
    data object NotRequired : PasswordRequirement
    data class Failure(val failure: AuthenticationFailure) : PasswordRequirement
}

sealed interface RegisterDeviceResult<out SessionT> {
    data class Success<SessionT>(
        val initialSyncCompleted: Boolean,
        val isE2EIRequired: Boolean,
        val e2eiSessionId: SessionT? = null,
    ) : RegisterDeviceResult<SessionT>

    data object TooManyDevices : RegisterDeviceResult<Nothing>
    data object MissingSecondFactor : RegisterDeviceResult<Nothing>
    data object InvalidSecondFactor : RegisterDeviceResult<Nothing>
    data object InvalidCredentials : RegisterDeviceResult<Nothing>
    data object PasswordRequired : RegisterDeviceResult<Nothing>
    data class Failure(val failure: AuthenticationFailure) : RegisterDeviceResult<Nothing>
}

sealed interface RequestVerificationCodeResult {
    data class Sent(val email: String) : RequestVerificationCodeResult
    data class TooManyRequests(val email: String) : RequestVerificationCodeResult
    data object MissingEmail : RequestVerificationCodeResult
    data class Failure(val failure: AuthenticationFailure) : RequestVerificationCodeResult
}

enum class AuthenticationFailure {
    NoNetwork,
    ServerMiscommunication,
    Unknown,
}

fun interface RegisterDeviceResendTimer {
    suspend fun start(
        seconds: Long,
        onUpdate: (String) -> Unit,
        onFinish: () -> Unit,
    )
}
