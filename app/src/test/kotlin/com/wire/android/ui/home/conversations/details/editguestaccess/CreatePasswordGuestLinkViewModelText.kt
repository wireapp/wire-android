/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.details.editguestaccess

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.config.ScopedArgsTestExtension
import com.wire.android.feature.GenerateRandomPasswordUseCase
import com.wire.android.ui.home.conversations.details.editguestaccess.createPasswordProtectedGuestLink.CreatePasswordGuestLinkNavArgs
import com.wire.android.ui.home.conversations.details.editguestaccess.createPasswordProtectedGuestLink.CreatePasswordGuestLinkViewModel
import com.wire.android.ui.navArgs
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.auth.ValidatePasswordResult
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.GenerateGuestRoomLinkResult
import com.wire.kalium.logic.feature.conversation.guestroomlink.GenerateGuestRoomLinkUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.amshove.kluent.internal.assertEquals
import org.amshove.kluent.internal.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(ScopedArgsTestExtension::class)
@ExtendWith(NavigationTestExtension::class)
class CreatePasswordGuestLinkViewModelText {

    @Test
    fun `given onPasswordUpdated called, when password is valid and password matches confirm, then isPasswordValid is marked as true`() {
        val (_, viewModel) = Arrangement()
            .withPasswordValidation(true)
            .arrange()

        viewModel.state = viewModel.state.copy(
            password = TextFieldValue("123"),
            passwordConfirm = TextFieldValue("password")
        )

        viewModel.onPasswordUpdated(TextFieldValue("password"))

        assertTrue(viewModel.state.isPasswordValid)
    }

    @Test
    fun `given onPasswordUpdated, when password is valid and doesn't match confirm, then isPasswordValid is marked as false`() {
        val (_, viewModel) = Arrangement()
            .withPasswordValidation(true)
            .arrange()

        viewModel.state = viewModel.state.copy(
            password = TextFieldValue("password"),
            passwordConfirm = TextFieldValue("password"),
            isPasswordValid = true
        )

        viewModel.onPasswordUpdated(TextFieldValue("123"))

        assertFalse(viewModel.state.isPasswordValid)
    }

    @Test
    fun `given onPasswordConfirmUpdated called, when the new password differ from the state, then isPasswordCopied is marked as false`() {
        val (_, viewModel) = Arrangement()
            .withPasswordValidation(true)
            .arrange()

        viewModel.state = viewModel.state.copy(
            passwordConfirm = TextFieldValue("old_password")
        )

        viewModel.onPasswordConfirmUpdated(TextFieldValue("new_password"))

        assertEquals(TextFieldValue("new_password"), viewModel.state.passwordConfirm)
    }

    @Test
    fun `given onPasswordConfirmUpdated, when the new password doesn't differ from the state, then isPasswordCopied is not changed`() {
        val (arrangement, viewModel) = Arrangement()
            .arrange()

        viewModel.state = viewModel.state.copy(
            passwordConfirm = TextFieldValue("password")
        )

        viewModel.onPasswordConfirmUpdated(TextFieldValue("password"))

        assertEquals(TextFieldValue("password"), viewModel.state.passwordConfirm)

        verify(exactly = 0) {
            arrangement.validatePassword(any())
        }
    }

    @Test
    fun `given onPasswordConfirmUpdated, when password is valid and matches confirm, then isPasswordValid is true`() {
        val (_, viewModel) = Arrangement()
            .withPasswordValidation(true)
            .arrange()

        viewModel.state = viewModel.state.copy(
            password = TextFieldValue("password"),
            passwordConfirm = TextFieldValue("123")
        )

        viewModel.onPasswordConfirmUpdated(TextFieldValue("password"))

        assertTrue(viewModel.state.isPasswordValid)
    }

    @Test
    fun `given onPasswordConfirmUpdated called, when password is valid and doesn't match confirm, then isPasswordValid is false`() {
        val (_, viewModel) = Arrangement()
            .withPasswordValidation(true)
            .arrange()

        viewModel.state = viewModel.state.copy(
            password = TextFieldValue("password"),
            passwordConfirm = TextFieldValue("password"),
            isPasswordValid = true
        )

        viewModel.onPasswordConfirmUpdated(TextFieldValue("123"))

        assertFalse(viewModel.state.isPasswordValid)
    }

    @Test
    fun `given onGenerateRandomPassword called, when password is generated, then password and passwordConfirm are updated`() {
        val (_, viewModel) = Arrangement()
            .withGenerateRandomPassword("password")
            .withPasswordValidation(true)
            .arrange()

        viewModel.state = viewModel.state.copy(
            password = TextFieldValue("123"),
            passwordConfirm = TextFieldValue("123"),
            isPasswordValid = false
        )

        viewModel.onGenerateRandomPassword()

        assertTrue(viewModel.state.password.text.isNotEmpty())
        assertTrue(viewModel.state.passwordConfirm.text.isNotEmpty())
        assertEquals(viewModel.state.password, viewModel.state.passwordConfirm)
        assertTrue(viewModel.state.isPasswordValid)
    }

    @Test
    fun `given onGenerateLink called, when link is generated, then isLinkCreationSuccessful is marked as true`() {
        val (_, viewModel) = Arrangement()
            .withGenerateGuestLink(
                GenerateGuestRoomLinkResult.Success
            )
            .arrange()

        viewModel.state = viewModel.state.copy(
            password = TextFieldValue("password"),
            passwordConfirm = TextFieldValue("password"),
            isPasswordValid = true
        )

        viewModel.onGenerateLink()

        assertTrue(viewModel.state.isLinkCreationSuccessful)
    }

    @Test
    fun `given onGenerateLink called, when link is not generated, then isLinkCreationSuccessful is marked as false`() {
        val expectedError = NetworkFailure.NoNetworkConnection(null)
        val (_, viewModel) = Arrangement()
            .withGenerateGuestLink(
                GenerateGuestRoomLinkResult.Failure(expectedError)
            )
            .arrange()

        viewModel.state = viewModel.state.copy(
            password = TextFieldValue("password"),
            passwordConfirm = TextFieldValue("password"),
            isPasswordValid = true
        )

        viewModel.onGenerateLink()

        assertFalse(viewModel.state.isLinkCreationSuccessful)
        assertEquals(
            expectedError,
            viewModel.state.error
        )
    }

    private companion object {
        val CONVERSATION_ID = ConversationId("conv_id", "conv_domain")
    }

    private class Arrangement {

        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        @MockK
        lateinit var generateGuestRoomLink: GenerateGuestRoomLinkUseCase

        @MockK
        lateinit var validatePassword: ValidatePasswordUseCase

        @MockK
        lateinit var generateRandomPasswordUseCase: GenerateRandomPasswordUseCase

        init {
            MockKAnnotations.init(this)
            every {
                savedStateHandle.navArgs<CreatePasswordGuestLinkNavArgs>()
            } returns CreatePasswordGuestLinkNavArgs(
                conversationId = CONVERSATION_ID
            )
        }

        fun withPasswordValidation(result: Boolean) = apply {
            every {
                validatePassword(any())
            } returns if (result) ValidatePasswordResult.Valid else ValidatePasswordResult.Invalid()
        }

        fun withGenerateGuestLink(
            result: GenerateGuestRoomLinkResult
        ) = apply {
            coEvery {
                generateGuestRoomLink(any(), any())
            } returns result
        }

        fun withGenerateRandomPassword(
            result: String
        ) = apply {
            every {
                generateRandomPasswordUseCase()
            } returns result
        }

        private val viewModel: CreatePasswordGuestLinkViewModel = CreatePasswordGuestLinkViewModel(
            generateGuestRoomLink = generateGuestRoomLink,
            validatePassword = validatePassword,
            generateRandomPasswordUseCase = generateRandomPasswordUseCase,
            savedStateHandle = savedStateHandle
        )

        fun arrange() = this to viewModel
    }
}
