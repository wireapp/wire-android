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
package com.wire.ios.shared.auth.email

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
        assertEquals("", state.proxyIdentifier)
        assertEquals("", state.proxyPassword)
        assertTrue(state.userIdentifierEnabled)
        assertFalse(state.loginEnabled)
        assertIs<LoginEmailFlowState.Default>(state.flowState)
        assertEquals(LoginEmailVerificationCodeState.DEFAULT_VERIFICATION_CODE_LENGTH, state.secondFactorVerificationCode.codeLength)
        assertFalse(state.secondFactorVerificationCode.isCodeInputNecessary)
        assertFalse(state.secondFactorVerificationCode.isCurrentCodeInvalid)
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
