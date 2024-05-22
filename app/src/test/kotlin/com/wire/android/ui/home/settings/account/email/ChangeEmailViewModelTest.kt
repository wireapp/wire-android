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
package com.wire.android.ui.home.settings.account.email

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.config.mockUri
import com.wire.android.framework.TestUser
import com.wire.android.ui.home.settings.account.email.updateEmail.ChangeEmailState
import com.wire.android.ui.home.settings.account.email.updateEmail.ChangeEmailViewModel
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.UpdateEmailUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okio.IOException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class, SnapshotExtension::class)
class ChangeEmailViewModelTest {

    @Test
    fun `given updateEmail returns success with VerificationEmailSent, when updateEmail is called, then navigate to VerifyEmail`() =
        runTest {
            val newEmail = "newEmail"
            val (_, viewModel) = Arrangement()
                .withNewEmail(newEmail)
                .withUpdateEmailResult(UpdateEmailUseCase.Result.Success.VerificationEmailSent)
                .arrange()

            viewModel.onSaveClicked()

            assertInstanceOf(ChangeEmailState.FlowState.Success::class.java, viewModel.state.flowState).also {
                assertEquals(newEmail, it.newEmail)
            }
        }

    @Test
    fun `given updateEmail returns success with NoChange, when updateEmail is called, then navigate back`() = runTest {
        val (_, viewModel) = Arrangement()
            .withNewEmail("newEmail")
            .withUpdateEmailResult(UpdateEmailUseCase.Result.Success.NoChange)
            .arrange()

        viewModel.onSaveClicked()

        assertInstanceOf(ChangeEmailState.FlowState.NoChange::class.java, viewModel.state.flowState)
    }

    @Test
    fun `given update EmailAlreadyInUse error is returned, when onSaveClicked is called, then show error state is updated`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withNewEmail("newEmail")
            .withUpdateEmailResult(UpdateEmailUseCase.Result.Failure.EmailAlreadyInUse)
            .arrange()

        viewModel.onSaveClicked()

        assertInstanceOf(ChangeEmailState.FlowState.Error.TextFieldError.AlreadyInUse::class.java, viewModel.state.flowState)
        coVerify(exactly = 1) { arrangement.updateEmail(any()) }
    }

    @Test
    fun `given update error is returned, when onSaveClicked is called, then show error state is updated`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withNewEmail("newEmail")
            .withUpdateEmailResult(UpdateEmailUseCase.Result.Failure.GenericFailure(NetworkFailure.NoNetworkConnection(IOException())))
            .arrange()

        viewModel.onSaveClicked()

        assertInstanceOf(ChangeEmailState.FlowState.Error.TextFieldError.Generic::class.java, viewModel.state.flowState)
        coVerify(exactly = 1) { arrangement.updateEmail(any()) }
    }

    @Test
    fun `given update EmailInvalid error is returned, when onSaveClicked is called, then show error state is updated`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withNewEmail("newEmail")
            .withUpdateEmailResult(UpdateEmailUseCase.Result.Failure.InvalidEmail)
            .arrange()

        viewModel.onSaveClicked()

        assertInstanceOf(ChangeEmailState.FlowState.Error.TextFieldError.InvalidEmail::class.java, viewModel.state.flowState)
        coVerify(exactly = 1) { arrangement.updateEmail(any()) }
    }

    private class Arrangement {

        @MockK
        lateinit var updateEmail: UpdateEmailUseCase

        @MockK
        lateinit var self: GetSelfUserUseCase

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            mockUri()

            coEvery { self() } returns flowOf(TestUser.SELF_USER)
        }

        private val viewModel = ChangeEmailViewModel(
            updateEmail,
            self
        )

        fun withNewEmail(newEmail: String) = apply {
            viewModel.textState.setTextAndPlaceCursorAtEnd(newEmail)
        }

        fun withUpdateEmailResult(result: UpdateEmailUseCase.Result) = apply {
            coEvery { updateEmail(any()) } returns result
        }

        fun arrange() = this to viewModel
    }
}
