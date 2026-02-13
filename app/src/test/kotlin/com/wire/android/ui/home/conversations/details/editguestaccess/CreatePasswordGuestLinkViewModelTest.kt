/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.config.NavigationTestExtension
import com.wire.android.ui.home.conversations.details.editguestaccess.createPasswordProtectedGuestLink.CreatePasswordGuestLinkNavArgs
import com.wire.android.ui.home.conversations.details.editguestaccess.createPasswordProtectedGuestLink.CreatePasswordGuestLinkViewModel
import com.ramcosta.composedestinations.generated.app.navArgs
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.auth.ValidatePasswordResult
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.GenerateGuestRoomLinkResult
import com.wire.kalium.logic.feature.conversation.guestroomlink.GenerateGuestRoomLinkUseCase
import com.wire.kalium.logic.util.RandomPassword
import io.mockk.MockKAnnotations
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(
    NavigationTestExtension::class
)
class CreatePasswordGuestLinkViewModelTest {

    private val dispatcher: TestDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given password confirm emitted new value, when the new value is not different, then validate is not called`() {
        val (arrangement, viewModel) = Arrangement(dispatcher)
            .withPasswordValidation(true)
            .arrange()
        viewModel.state.confirmPasswordTextState.setTextAndPlaceCursorAtEnd("password")
        arrangement.clearValidatePasswordCallsCount()

        viewModel.state.confirmPasswordTextState.setTextAndPlaceCursorAtEnd("password")

        assertEquals("password", viewModel.state.confirmPasswordTextState.text.toString())

        verify(exactly = 0) {
            arrangement.validatePassword(any())
        }
    }

    @Test
    fun `given onGenerateLink called, when password is valid and matches confirm, then invalidPassword is false`() = runTest(dispatcher) {
        val (arrangement, viewModel) = Arrangement(dispatcher)
            .withPasswordValidation(true)
            .withGenerateGuestLink(GenerateGuestRoomLinkResult.Success)
            .arrange()

        viewModel.state.passwordTextState.setTextAndPlaceCursorAtEnd("password")
        viewModel.state.confirmPasswordTextState.setTextAndPlaceCursorAtEnd("password")

        viewModel.suspendGenerateGuestRoomLink()

        assertFalse(viewModel.state.invalidPassword)
        coVerify(exactly = 1) {
            arrangement.validatePassword(any())
            arrangement.generateGuestRoomLink(any(), any())
        }
    }

    @Test
    fun `given onGenerateRandomPassword called, when password is generated, then password and passwordConfirm are updated`() {
        val (_, viewModel) = Arrangement(dispatcher)
            .withGenerateRandomPassword("generated_password")
            .withPasswordValidation(true)
            .arrange()

        viewModel.state.passwordTextState.setTextAndPlaceCursorAtEnd("password")
        viewModel.state.confirmPasswordTextState.setTextAndPlaceCursorAtEnd("password")

        viewModel.onGenerateRandomPassword()

        assertEquals("generated_password", viewModel.state.passwordTextState.text.toString())
        assertEquals(viewModel.state.passwordTextState.text, viewModel.state.confirmPasswordTextState.text)
        assertFalse(viewModel.state.invalidPassword)
    }

    @Test
    fun `given onGenerateLink called, when link is generated, then isLinkCreationSuccessful is marked as true`() = runTest(dispatcher) {
        val (_, viewModel) = Arrangement(dispatcher)
            .withPasswordValidation(true)
            .withGenerateGuestLink(GenerateGuestRoomLinkResult.Success)
            .arrange()

        viewModel.state.passwordTextState.setTextAndPlaceCursorAtEnd("password!123456")
        viewModel.state.confirmPasswordTextState.setTextAndPlaceCursorAtEnd("password!123456")
        viewModel.state = viewModel.state.copy(invalidPassword = true)

        viewModel.suspendGenerateGuestRoomLink()

        assertEquals(true, viewModel.state.isLinkCreationSuccessful)
    }

    @Test
    fun `given onGenerateLink called, when link is not generated, then isLinkCreationSuccessful is marked as false`() =
        runTest(dispatcher) {
            val expectedError = NetworkFailure.NoNetworkConnection(null)
            val (_, viewModel) = Arrangement(dispatcher)
                .withPasswordValidation(true)
                .withGenerateGuestLink(GenerateGuestRoomLinkResult.Failure(expectedError))
                .arrange()

            viewModel.state.passwordTextState.setTextAndPlaceCursorAtEnd("password")
            viewModel.state.confirmPasswordTextState.setTextAndPlaceCursorAtEnd("password")
            viewModel.state = viewModel.state.copy(invalidPassword = true)

            viewModel.suspendGenerateGuestRoomLink()

            assertEquals(false, viewModel.state.isLinkCreationSuccessful)
            assertEquals(
                expectedError,
                viewModel.state.error
            )
        }

    @Test
    fun `given password is invalid, when password changes, then isPasswordValid state is set to false`() = runTest(dispatcher) {
        val (_, viewModel) = Arrangement(dispatcher)
            .withObservePasswordChanges()
            .withPasswordValidation(false)
            .withInvalidPasswordState()
            .arrange()

        viewModel.suspendGenerateGuestRoomLink()

        assertTrue(viewModel.state.invalidPassword)

        viewModel.state.passwordTextState.edit { append("1") }
        delay(100)
        assertFalse(viewModel.state.invalidPassword)
    }

    @Test
    fun `given password and confirm password does not match, when clicking on generate link, then isPasswordValid is marked as false and link not generated`() =
        runTest(dispatcher) {
            val (arrangement, viewModel) = Arrangement(dispatcher)
                .withObservePasswordChanges()
                .withPasswordValidation(false)
                .arrange()

            viewModel.state.passwordTextState.setTextAndPlaceCursorAtEnd("password")
            viewModel.state.confirmPasswordTextState.setTextAndPlaceCursorAtEnd("password1")

            viewModel.suspendGenerateGuestRoomLink()

            assertTrue(viewModel.state.invalidPassword)
            assertFalse(viewModel.state.isLinkCreationSuccessful)

            coVerify(exactly = 0) {
                arrangement.generateGuestRoomLink(any(), any())
                arrangement.validatePassword(any())
            }
        }

    @Test
    fun `given password and confirm match but empty, when clicking on generate link, then isPasswordValid is marked as false and link not generated`() =
        runTest(dispatcher) {
            val (arrangement, viewModel) = Arrangement(dispatcher)
                .withObservePasswordChanges()
                .arrange()

            viewModel.state.passwordTextState.setTextAndPlaceCursorAtEnd("password")
            viewModel.state.confirmPasswordTextState.setTextAndPlaceCursorAtEnd("")

            viewModel.suspendGenerateGuestRoomLink()

            assertTrue(viewModel.state.invalidPassword)
            assertFalse(viewModel.state.isLinkCreationSuccessful)

            coVerify(exactly = 0) {
                arrangement.generateGuestRoomLink(any(), any())
                arrangement.validatePassword(any())
            }
        }

    @Test
    fun `given a valid password and confirm, when clicking on generate link, then link is generated`() = runTest {
        val (arrangement, viewModel) = Arrangement(dispatcher)
            .withObservePasswordChanges()
            .withPasswordValidation(true)
            .withGenerateGuestLink(GenerateGuestRoomLinkResult.Success)
            .arrange()

        viewModel.state.passwordTextState.setTextAndPlaceCursorAtEnd("password")
        viewModel.state.confirmPasswordTextState.setTextAndPlaceCursorAtEnd("password")

        viewModel.suspendGenerateGuestRoomLink()

        assertFalse(viewModel.state.invalidPassword)
        assertTrue(viewModel.state.isLinkCreationSuccessful)

        coVerify(exactly = 1) {
            arrangement.generateGuestRoomLink(any(), any())
            arrangement.validatePassword(any())
        }
    }

    @Test
    fun `given a invalid password and confirm, when clicking on generate link, then link is generated`() = runTest {
        val (arrangement, viewModel) = Arrangement(dispatcher)
            .withObservePasswordChanges()
            .withPasswordValidation(false)
            .arrange()

        viewModel.state.passwordTextState.setTextAndPlaceCursorAtEnd("password")
        viewModel.state.confirmPasswordTextState.setTextAndPlaceCursorAtEnd("password")

        viewModel.suspendGenerateGuestRoomLink()

        assertTrue(viewModel.state.invalidPassword)
        assertFalse(viewModel.state.isLinkCreationSuccessful)

        coVerify(exactly = 0) {
            arrangement.generateGuestRoomLink(any(), any())
        }
        coVerify(exactly = 1) {
            arrangement.validatePassword(any())
        }
    }

    private companion object {
        val CONVERSATION_ID = ConversationId("conv_id", "conv_domain")
    }

    private class Arrangement(private val dispatcher: TestDispatcher) {

        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        @MockK
        lateinit var generateGuestRoomLink: GenerateGuestRoomLinkUseCase

        @MockK
        lateinit var validatePassword: ValidatePasswordUseCase

        @MockK
        lateinit var generateRandomPassword: RandomPassword

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

        fun withInvalidPasswordState() = apply {
            viewModel.state = viewModel.state.copy(invalidPassword = true)
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
                generateRandomPassword()
            } returns result
        }

        fun withObservePasswordChanges() = apply {
            viewModel.viewModelScope.launch(dispatcher) {
                viewModel.observePasswordValidation()
            }
        }

        private val viewModel: CreatePasswordGuestLinkViewModel by lazy {
            CreatePasswordGuestLinkViewModel(
                generateGuestRoomLink = generateGuestRoomLink,
                validatePassword = validatePassword,
                generatePassword = generateRandomPassword,
                savedStateHandle = savedStateHandle
            )
        }

        fun clearValidatePasswordCallsCount() = clearMocks(
            validatePassword,
            answers = false,
            recordedCalls = true,
            childMocks = false,
            verificationMarks = false,
            exclusionRules = false
        )

        fun arrange() = this to viewModel
    }
}
