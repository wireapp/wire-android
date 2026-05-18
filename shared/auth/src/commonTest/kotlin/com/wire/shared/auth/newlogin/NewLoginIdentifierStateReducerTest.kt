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
package com.wire.shared.auth.newlogin

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NewLoginIdentifierStateReducerTest {

    @Test
    fun givenDefaultState_whenUserIdentifierIsEntered_thenNextIsEnabled() {
        val state = NewLoginIdentifierState().withUserIdentifier("user@example.com")

        assertEquals("user@example.com", state.userIdentifier)
        assertTrue(state.nextEnabled)
        assertEquals(NewLoginIdentifierFlowState.Default, state.flowState)
        assertFalse(state.isLoading)
        assertFalse(state.hasTextFieldError)
        assertNull(state.textFieldError)
        assertFalse(state.hasDialogError)
        assertNull(state.dialogError)
        assertFalse(state.isCustomConfigDialogVisible)
        assertNull(state.customConfigServerLinks)
    }

    @Test
    fun givenTextFieldError_whenUserIdentifierChanges_thenErrorIsCleared() {
        val state = NewLoginIdentifierState(
            flowState = NewLoginIdentifierFlowState.TextFieldError(NewLoginIdentifierTextFieldError.InvalidValue),
        ).withUserIdentifier("user@example.com")

        assertEquals(NewLoginIdentifierFlowState.Default, state.flowState)
        assertFalse(state.hasTextFieldError)
        assertNull(state.textFieldError)
    }

    @Test
    fun givenUserIdentifier_whenLoadingStarts_thenNextIsDisabled() {
        val state = NewLoginIdentifierState(userIdentifier = "user@example.com")
            .withFlowState(NewLoginIdentifierFlowState.Loading)

        assertFalse(state.nextEnabled)
        assertTrue(state.isLoading)
    }

    @Test
    fun givenTextFieldErrorState_whenReadByPlatform_thenConvenienceFieldsAreExposed() {
        val state = NewLoginIdentifierState(
            flowState = NewLoginIdentifierFlowState.TextFieldError(NewLoginIdentifierTextFieldError.InvalidValue),
        )

        assertTrue(state.hasTextFieldError)
        assertEquals(NewLoginIdentifierTextFieldError.InvalidValue, state.textFieldError)
        assertFalse(state.hasDialogError)
        assertNull(state.dialogError)
    }

    @Test
    fun givenDialogErrorState_whenReadByPlatform_thenConvenienceFieldsAreExposed() {
        val error = NewLoginIdentifierDialogError.SSOResultFailure(NewLoginSsoFailureCode.InvalidCode)
        val state = NewLoginIdentifierState(
            flowState = NewLoginIdentifierFlowState.DialogError(error),
        )

        assertTrue(state.hasDialogError)
        assertEquals(error, state.dialogError)
        assertFalse(state.hasTextFieldError)
        assertNull(state.textFieldError)
    }
}
