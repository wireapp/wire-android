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
package com.wire.shared.auth.email

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LoginEmailContractTest {

    @Test
    fun givenDefaultState_whenCreated_thenMatchesLoginEmailDefaults() {
        val state = LoginEmailState()

        assertEquals("", state.userIdentifier)
        assertEquals("", state.password)
        assertTrue(state.isPasswordEmpty)
        assertFalse(state.isPasswordNotEmpty)
        assertEquals("", state.proxyIdentifier)
        assertEquals("", state.proxyPassword)
        assertTrue(state.userIdentifierEnabled)
        assertFalse(state.loginEnabled)
        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        assertFalse(state.isInvalidCredentials)
        assertIs<LoginEmailFlowState.Default>(state.flowState)
        assertEquals(LoginEmailVerificationCodeState.DEFAULT_VERIFICATION_CODE_LENGTH, state.secondFactorVerificationCode.codeLength)
        assertFalse(state.secondFactorVerificationCode.isCodeInputNecessary)
        assertFalse(state.secondFactorVerificationCode.isCurrentCodeInvalid)
        assertFalse(state.isSecondFactorRequired)
        assertFalse(state.isSecondFactorInvalid)
        assertEquals("", state.secondFactorCode)
        assertFalse(state.isSecondFactorCodeComplete)
        assertEquals("", state.secondFactorEmail)
    }

    @Test
    fun givenPasswordAndSecondFactorState_whenCreated_thenExposesPlatformFriendlyUiFlags() {
        val state = LoginEmailState(
            password = "password",
            flowState = LoginEmailFlowState.Error(LoginEmailError.InvalidCredentials),
            secondFactorVerificationCode = LoginEmailVerificationCodeState(
                code = "123456",
                emailUsed = "user@example.com",
                isCodeInputNecessary = true,
                isCurrentCodeInvalid = true,
            ),
        )

        assertFalse(state.isPasswordEmpty)
        assertTrue(state.isPasswordNotEmpty)
        assertTrue(state.isInvalidCredentials)
        assertTrue(state.isSecondFactorRequired)
        assertTrue(state.isSecondFactorInvalid)
        assertEquals("123456", state.secondFactorCode)
        assertTrue(state.isSecondFactorCodeComplete)
        assertEquals("user@example.com", state.secondFactorEmail)
    }

    @Test
    fun givenSubmitLoginIntent_whenCreatedWithoutOverride_thenUsernameIsAllowed() {
        val intent = LoginEmailIntent.SubmitLogin()

        assertTrue(intent.usernameAllowed)
    }

    @Test
    fun givenSuccessEffect_whenCreated_thenCarriesNavigationFlags() {
        val effect = LoginEmailEffect.LoginSucceeded(
            initialSyncCompleted = true,
            isE2EIRequired = false,
        )

        assertTrue(effect.initialSyncCompleted)
        assertFalse(effect.isE2EIRequired)
    }
}
