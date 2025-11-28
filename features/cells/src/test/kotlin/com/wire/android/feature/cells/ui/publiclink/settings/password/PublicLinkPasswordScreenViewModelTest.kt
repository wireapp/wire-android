/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.feature.cells.ui.publiclink.settings.password

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.wire.android.config.NavigationTestExtension
import com.wire.android.feature.cells.ui.navArgs
import com.wire.kalium.cells.domain.model.PublicLink
import com.wire.kalium.cells.domain.usecase.publiclink.CreatePublicLinkPasswordUseCase
import com.wire.kalium.cells.domain.usecase.publiclink.GetPublicLinkPasswordUseCase
import com.wire.kalium.cells.domain.usecase.publiclink.UpdatePublicLinkPasswordUseCase
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.functional.left
import com.wire.kalium.common.functional.right
import com.wire.kalium.logic.util.RandomPassword
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(NavigationTestExtension::class)
class PublicLinkPasswordScreenViewModelTest {

    private companion object {
        const val randomPassword = "random_password"
        val testLink = PublicLink(
            uuid = "linkUuid",
            url = "https://publicLink.com",
        )
    }

    @BeforeEach
    fun beforeEach() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun afterEach() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given link password enabled when view model is created then correct initial state is emitted`() = runTest {
        val (_, viewModel) = Arrangement()
            .withPasswordEnabled(true)
            .withLocalPassword()
            .arrange()

        viewModel.state.test {
            assertTrue(awaitItem().isEnabled)
        }
    }

    @Test
    fun `given link password disabled when view model is created then correct initial state is emitted`() = runTest {
        val (_, viewModel) = Arrangement()
            .withPasswordEnabled(false)
            .withLocalPassword()
            .arrange()

        viewModel.state.test {
            assertFalse(awaitItem().isEnabled)
        }
    }

    @Test
    fun `given link password disabled when enabled then correct state is emitted`() = runTest {
        val (_, viewModel) = Arrangement()
            .withPasswordEnabled(false)
            .withLocalPassword()
            .arrange()

        viewModel.state.test {
            skipItems(1)
            viewModel.onEnableClick()
            val state = awaitItem()

            assertTrue(state.isEnabled)
            assertTrue(state.screenState == PasswordScreenState.SETUP_PASSWORD)
        }
    }

    @Test
    fun `given link password enabled when disabled then confirmation dialog is shown`() = runTest {
        val (_, viewModel) = Arrangement()
            .withPasswordEnabled(true)
            .withPasswordRemoveSuccess()
            .withLocalPassword("test")
            .arrange()

        viewModel.actions.test {
            viewModel.onEnableClick()
            assertEquals(ShowRemoveConfirmationDialog, awaitItem())
        }
    }

    @Test
    fun `given link password enabled when disable confirmed then remove use case called`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withPasswordEnabled(true)
            .withPasswordRemoveSuccess()
            .withLocalPassword()
            .arrange()

        viewModel.onConfirmPasswordRemoval(true)

        coVerify(exactly = 1) {
            arrangement.updatePassword(
                linkUuid = testLink.uuid,
                password = null
            )
        }
    }

    @Test
    fun `given no link password when enabled and disabled then remove use case not called`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withPasswordEnabled(false)
            .withPasswordRemoveSuccess()
            .withLocalPassword()
            .arrange()

        viewModel.onEnableClick()
        viewModel.onEnableClick()

        coVerify(exactly = 0) {
            arrangement.updatePassword(any(), any())
        }
    }

    @Test
    fun `given remove password success when disabled then correct state is emitted`() = runTest {
        val (_, viewModel) = Arrangement()
            .withPasswordEnabled(true)
            .withPasswordRemoveSuccess()
            .withLocalPassword()
            .arrange()

        viewModel.state.test {
            skipItems(1)
            viewModel.onConfirmPasswordRemoval(true)
            val state = awaitItem()
            assertFalse(state.isEnabled)
            assertEquals(PasswordScreenState.SETUP_PASSWORD, state.screenState)
            assertFalse(viewModel.isPasswordCreated)
            assertEquals("", viewModel.passwordTextState.text)
        }
    }

    @Test
    fun `given remove password failure when disabled then correct state is emitted`() = runTest {
        val (_, viewModel) = Arrangement()
            .withPasswordEnabled(true)
            .withPasswordRemoveFailure()
            .withLocalPassword()
            .arrange()

        viewModel.state.test {
            viewModel.onConfirmPasswordRemoval(true)
            val state = awaitItem()
            assertTrue(state.isEnabled)
        }
    }

    @Test
    fun `given failure when remove password then error message is shown`() = runTest {
        val (_, viewModel) = Arrangement()
            .withPasswordEnabled(true)
            .withPasswordRemoveFailure()
            .withLocalPassword("test")
            .arrange()

        viewModel.actions.test {
            viewModel.onConfirmPasswordRemoval(true)
            assertEquals(ShowError(PasswordError.RemoveFailure), awaitItem())
        }
    }

    @Test
    fun `given random password when password generated then use case is called and text updated`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withPasswordEnabled(true)
            .withLocalPassword()
            .arrange()

        viewModel.generatePassword()

        verify(exactly = 1) { arrangement.generateRandomPassword() }
        assertEquals(randomPassword, viewModel.passwordTextState.text)
    }

    @Test
    fun `given password entered when applying then progress state emitted`() = runTest {
        val (_, viewModel) = Arrangement()
            .withPasswordEnabled(true)
            .withPasswordUpdateSuccess()
            .withLocalPassword()
            .arrange()

        viewModel.state.test {
            skipItems(1)
            viewModel.setPassword()
            assertTrue(awaitItem().showProgress)
        }
    }

    @Test
    fun `given password already set when applying password then update is called`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withPasswordEnabled(true)
            .withPasswordUpdateSuccess()
            .withLocalPassword()
            .arrange()

        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("test")

        viewModel.setPassword()

        coVerify(exactly = 1) {
            arrangement.updatePassword(testLink.uuid, "test")
        }
    }

    @Test
    fun `given no password set when applying password then create is called`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withPasswordEnabled(false)
            .withPasswordCreateSuccess()
            .arrange()

        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("test")

        viewModel.setPassword()

        coVerify(exactly = 1) {
            arrangement.createPassword(testLink.uuid, "test")
        }
    }

    @Test
    fun `given password update success when applying password then correct action is emitted`() = runTest {
        val (_, viewModel) = Arrangement()
            .withPasswordEnabled(true)
            .withPasswordUpdateSuccess()
            .withLocalPassword("test")
            .arrange()

        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("test")

        viewModel.actions.test {
            viewModel.setPassword()
            val action = awaitItem()
            assertTrue(action is CopyPasswordAndClose)
            assertEquals("test", (action as CopyPasswordAndClose).password)
        }
    }

    @Test
    fun `given password update failure when applying password then correct state is emitted`() = runTest {
        val (_, viewModel) = Arrangement()
            .withPasswordEnabled(true)
            .withPasswordUpdateFailure()
            .withLocalPassword()
            .arrange()

        viewModel.state.test {
            viewModel.setPassword()
            assertFalse(awaitItem().showProgress)
        }
    }

    @Test
    fun `given password update failure when disabling password then error message is shown`() = runTest {
        val (_, viewModel) = Arrangement()
            .withPasswordEnabled(true)
            .withPasswordUpdateFailure()
            .withLocalPassword("test")
            .arrange()

        viewModel.actions.test {
            viewModel.onConfirmPasswordRemoval(true)
            assertEquals(ShowError(PasswordError.RemoveFailure), awaitItem())
        }
    }

    @Test
    fun `given password is set when resetting password then correct state is emitted`() = runTest {
        val (_, viewModel) = Arrangement()
            .withPasswordEnabled(true)
            .withPasswordUpdateFailure()
            .withLocalPassword()
            .arrange()

        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("test")

        viewModel.state.test {
            skipItems(1)
            viewModel.resetPassword()
            val state = awaitItem()
            assertFalse(state.isPasswordValid)
            assertEquals(PasswordScreenState.SETUP_PASSWORD, state.screenState)
            assertEquals("", viewModel.passwordTextState.text)
        }
    }

    @Test
    fun `given password is set and local password available when initializing then correct state is emitted`() = runTest {
        val (_, viewModel) = Arrangement()
            .withPasswordEnabled(true)
            .withLocalPassword("local_password")
            .arrange()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(PasswordScreenState.AVAILABLE, state.screenState)
            assertEquals("local_password", viewModel.passwordTextState.text)
        }
    }

    @Test
    fun `given password is set and local password not available when initializing then correct state is emitted`() = runTest {
        val (_, viewModel) = Arrangement()
            .withPasswordEnabled(true)
            .withLocalPassword(null)
            .arrange()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(PasswordScreenState.NOT_AVAILABLE, state.screenState)
        }
    }

    @Test
    fun `given password is set and local password not available when initializing then dialog is shown`() = runTest {
        val (_, viewModel) = Arrangement()
            .withPasswordEnabled(true)
            .withLocalPassword(null)
            .arrange()

        viewModel.actions.test {
            assertEquals(ShowMissingPasswordDialog, awaitItem())
        }
    }

    private class Arrangement {

        init {

            MockKAnnotations.init(this, relaxUnitFun = true)

            every { savedStateHandle.navArgs<PublicLinkPasswordNavArgs>() } returns PublicLinkPasswordNavArgs(
                linkUuid = testLink.uuid,
                passwordEnabled = false,
            )
        }

        fun withPasswordEnabled(enabled: Boolean) = apply {
            every { savedStateHandle.navArgs<PublicLinkPasswordNavArgs>() } returns PublicLinkPasswordNavArgs(
                linkUuid = testLink.uuid,
                passwordEnabled = enabled,
            )
        }

        fun withPasswordRemoveSuccess() = apply {
            coEvery { updatePassword(any(), any()) } returns Unit.right()
        }

        fun withPasswordRemoveFailure() = apply {
            coEvery { updatePassword(any(), any()) } returns
                    CoreFailure.Unknown(IllegalStateException("Test")).left()
        }

        fun withPasswordCreateSuccess() = apply {
            coEvery { createPassword(any(), any()) } returns Unit.right()
        }

        fun withPasswordUpdateSuccess() = apply {
            coEvery { updatePassword(any(), any()) } returns Unit.right()
        }

        fun withPasswordUpdateFailure() = apply {
            coEvery { updatePassword(any(), any()) } returns
                    CoreFailure.Unknown(IllegalStateException("Test")).left()
        }

        fun withLocalPassword(password: String? = null) = apply {
            coEvery { getLocalPassword(any()) } returns password
        }

        @MockK
        lateinit var generateRandomPassword: RandomPassword

        @MockK
        lateinit var createPassword: CreatePublicLinkPasswordUseCase

        @MockK
        lateinit var updatePassword: UpdatePublicLinkPasswordUseCase

        @MockK
        lateinit var getLocalPassword: GetPublicLinkPasswordUseCase

        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        fun arrange(): Pair<Arrangement, PublicLinkPasswordScreenViewModel> {

            every { generateRandomPassword() } returns randomPassword

            return this to PublicLinkPasswordScreenViewModel(
                generateRandomPassword = generateRandomPassword,
                createPassword = createPassword,
                updatePassword = updatePassword,
                getPublicLinkPassword = getLocalPassword,
                savedStateHandle = savedStateHandle
            )
        }
    }
}
