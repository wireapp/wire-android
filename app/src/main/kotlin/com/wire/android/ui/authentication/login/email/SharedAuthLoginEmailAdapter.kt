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

package com.wire.android.ui.authentication.login.email

import com.wire.android.ui.authentication.login.LoginState
import com.wire.android.ui.authentication.verificationcode.VerificationCodeState
import com.wire.kalium.logic.configuration.server.ServerConfig

/**
 * Android-side boundary for replacing the email/password login step with shared/auth.
 *
 * A future implementation should adapt shared/auth state and effects to the existing Android UI contract.
 * Android keeps text fields, resources, navigation, proxy form rendering and screen lifecycle ownership.
 */
fun interface SharedAuthLoginEmailAdapter {
    suspend fun tryLogin(
        request: SharedAuthLoginEmailRequest,
        callbacks: SharedAuthLoginEmailCallbacks,
    ): Boolean
}

data class SharedAuthLoginEmailRequest(
    val userIdentifier: String,
    val password: String,
    val secondFactorVerificationCode: String,
    val usernameAllowed: Boolean,
    val serverConfig: ServerConfig.Links,
)

interface SharedAuthLoginEmailCallbacks {
    suspend fun updateFlowState(flowState: LoginState)
    suspend fun updateSecondFactorState(update: (VerificationCodeState) -> VerificationCodeState)
    suspend fun startResendCodeTimer()
}

object LegacySharedAuthLoginEmailAdapter : SharedAuthLoginEmailAdapter {
    override suspend fun tryLogin(
        request: SharedAuthLoginEmailRequest,
        callbacks: SharedAuthLoginEmailCallbacks,
    ): Boolean = false
}
