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

package com.wire.android.ui.newauthentication.login

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LoginFlowStateHolderTest {

    @Test
    fun givenEmptyInput_whenCreated_thenNextIsDisabled() {
        val holder = LoginFlowStateHolder<Unit>()

        assertEquals(LoginFlowHolderState(), holder.state.value)
        assertFalse(holder.state.value.nextEnabled)
    }

    @Test
    fun givenNonEmptyInput_whenCreated_thenNextIsEnabled() {
        val holder = LoginFlowStateHolder<Unit>(initialUserIdentifier = "user@example.com")

        assertEquals("user@example.com", holder.userIdentifier)
        assertTrue(holder.state.value.nextEnabled)
    }

    @Test
    fun givenLoadingState_whenInputIsUpdated_thenNextStaysDisabled() {
        val holder = LoginFlowStateHolder<Unit>(initialFlowState = NewLoginFlowState.Loading)

        holder.updateUserIdentifier("user@example.com")

        assertEquals("user@example.com", holder.userIdentifier)
        assertEquals(NewLoginFlowState.Loading, holder.flowState)
        assertFalse(holder.state.value.nextEnabled)
    }

    @Test
    fun givenTextFieldError_whenInputIsUpdated_thenTextFieldErrorIsCleared() {
        val holder = LoginFlowStateHolder<Unit>(
            initialFlowState = NewLoginFlowState.Error.TextFieldError.InvalidValue
        )

        holder.updateUserIdentifier("user@example.com")

        assertEquals(NewLoginFlowState.Default, holder.flowState)
    }

    @Test
    fun givenDialogError_whenInputIsUpdated_thenDialogErrorIsKept() {
        val holder = LoginFlowStateHolder<Unit>(
            initialFlowState = NewLoginFlowState.Error.DialogError.InvalidSSOCode
        )

        holder.updateUserIdentifier("user@example.com")

        assertEquals(NewLoginFlowState.Error.DialogError.InvalidSSOCode, holder.flowState)
    }

    @Test
    fun givenResultCollector_whenResultIsEmitted_thenCollectorReceivesIt() = runTest {
        val holder = LoginFlowStateHolder<String>()

        holder.results.test {
            holder.emitResult("next")

            assertEquals("next", awaitItem())
        }
    }
}
