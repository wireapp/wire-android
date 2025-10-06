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
package com.wire.android.ui.legalhold.dialog.requested

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.ui.legalhold.dialog.requested.LegalHoldRequestedViewModelTest.Arrangement.Companion.UNKNOWN_ERROR
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.auth.ValidatePasswordResult
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.legalhold.ApproveLegalHoldRequestUseCase
import com.wire.kalium.logic.feature.legalhold.ObserveLegalHoldRequestUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import com.wire.android.assertions.shouldBeEqualTo
import com.wire.android.assertions.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class, SnapshotExtension::class)
class LegalHoldRequestedViewModelTest {

    @Test
    fun givenNoSession_whenGettingState_thenStateShouldBeHidden() = runTest {
        val (_, viewModel) = Arrangement()
            .withNotCurrentSession()
            .arrange()
        advanceUntilIdle()
        val state = viewModel.state
        state shouldBeInstanceOf LegalHoldRequestedState.Hidden::class
    }

    @Test
    fun givenSessionReturnsFailure_whenGettingState_thenStateShouldBeHidden() = runTest {
        val (_, viewModel) = Arrangement()
            .withCurrentSessionFailure()
            .arrange()
        advanceUntilIdle()
        viewModel.state shouldBeInstanceOf LegalHoldRequestedState.Hidden::class
    }

    @Test
    fun givenLegalHoldRequestReturnsFailure_whenGettingState_thenStateShouldBeHidden() = runTest {
        val (_, viewModel) = Arrangement()
            .withCurrentSessionExists()
            .withLegalHoldRequestResult(ObserveLegalHoldRequestUseCase.Result.Failure(UNKNOWN_ERROR))
            .arrange()
        advanceUntilIdle()
        viewModel.state shouldBeInstanceOf LegalHoldRequestedState.Hidden::class
    }

    @Test
    fun givenNoPendingLegalHoldRequest_whenGettingState_thenStateShouldBeHidden() = runTest {
        val (_, viewModel) = Arrangement()
            .withCurrentSessionExists()
            .withLegalHoldRequestResult(ObserveLegalHoldRequestUseCase.Result.NoLegalHoldRequest)
            .arrange()
        advanceUntilIdle()
        viewModel.state shouldBeInstanceOf LegalHoldRequestedState.Hidden::class
    }

    @Test
    fun givenPendingLegalHoldRequest_whenGettingState_thenStateShouldBeVisible() = runTest {
        val fingerprint = "fingerprint".toByteArray()
        val (_, viewModel) = Arrangement()
            .withCurrentSessionExists()
            .withLegalHoldRequestResult(ObserveLegalHoldRequestUseCase.Result.LegalHoldRequestAvailable(fingerprint))
            .withIsPasswordRequiredResult(IsPasswordRequiredUseCase.Result.Success(true))
            .arrange()
        advanceUntilIdle()
        val state = viewModel.state
        state shouldBeInstanceOf LegalHoldRequestedState.Visible::class
        state as LegalHoldRequestedState.Visible
        state.legalHoldDeviceFingerprint shouldBeEqualTo fingerprint.decodeToString()
    }

    @Test
    fun givenPendingLegalHoldRequestAndPasswordRequired_whenGettingState_thenShouldRequirePassword() = runTest {
        val fingerprint: String = "fingerprint"
        val (_, viewModel) = Arrangement()
            .withCurrentSessionExists()
            .withLegalHoldRequestResult(ObserveLegalHoldRequestUseCase.Result.LegalHoldRequestAvailable(fingerprint))
            .withIsPasswordRequiredResult(IsPasswordRequiredUseCase.Result.Success(true))
            .arrange()
        advanceUntilIdle()
        val state = viewModel.state
        state shouldBeInstanceOf LegalHoldRequestedState.Visible::class
        state as LegalHoldRequestedState.Visible
        state.requiresPassword shouldBeEqualTo true
        state.acceptEnabled shouldBeEqualTo false
    }

    @Test
    fun givenPendingLegalHoldRequestAndNoPasswordRequired_whenGettingState_thenShouldNotRequirePassword() = runTest {
        val fingerprint: String = "fingerprint"
        val (_, viewModel) = Arrangement()
            .withCurrentSessionExists()
            .withLegalHoldRequestResult(ObserveLegalHoldRequestUseCase.Result.LegalHoldRequestAvailable(fingerprint))
            .withIsPasswordRequiredResult(IsPasswordRequiredUseCase.Result.Success(false))
            .arrange()
        advanceUntilIdle()
        val state = viewModel.state
        state shouldBeInstanceOf LegalHoldRequestedState.Visible::class
        state as LegalHoldRequestedState.Visible
        state.requiresPassword shouldBeEqualTo false
        state.acceptEnabled shouldBeEqualTo true
    }

    private fun arrangeWithLegalHoldRequest(isPasswordRequired: Boolean = true) = Arrangement()
        .withCurrentSessionExists()
        .withLegalHoldRequestResult(ObserveLegalHoldRequestUseCase.Result.LegalHoldRequestAvailable("fingerprint"))
        .withIsPasswordRequiredResult(IsPasswordRequiredUseCase.Result.Success(isPasswordRequired))

    private fun LegalHoldRequestedState.assertStateVisible(assert: (LegalHoldRequestedState.Visible) -> Unit) {
        this shouldBeInstanceOf LegalHoldRequestedState.Visible::class
        this as LegalHoldRequestedState.Visible
        assert(this)
    }

    @Test
    fun givenInvalidPassword_whenEnteringPassword_thenAcceptButtonShouldBeDisabled() = runTest {
        val (_, viewModel) = arrangeWithLegalHoldRequest()
            .withValidatePasswordResult(ValidatePasswordResult.Invalid())
            .arrange()
        advanceUntilIdle()
        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("password")
        viewModel.state.assertStateVisible { it.acceptEnabled shouldBeEqualTo false }
    }

    @Test
    fun givenPasswordNotEmptyAndValid_whenEnteringPassword_thenAcceptButtonShouldBeEnabled() = runTest {
        val (_, viewModel) = arrangeWithLegalHoldRequest()
            .withValidatePasswordResult(ValidatePasswordResult.Valid)
            .arrange()
        advanceUntilIdle()
        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("password")
        viewModel.state.assertStateVisible { it.acceptEnabled shouldBeEqualTo true }
    }

    @Test
    fun givenNotNowChosen_whenApproving_thenStateShouldBeHidden() = runTest {
        val (_, viewModel) = arrangeWithLegalHoldRequest()
            .withValidatePasswordResult(ValidatePasswordResult.Invalid())
            .arrange()
        advanceUntilIdle()
        viewModel.notNowClicked()
        viewModel.state shouldBeInstanceOf LegalHoldRequestedState.Hidden::class
    }

    @Test
    fun givenInvalidPasswordResult_whenValidatingPassword_thenErrorStateShouldBeInvalidCredentials() = runTest {
        val (_, viewModel) = arrangeWithLegalHoldRequest()
            .withValidatePasswordResult(ValidatePasswordResult.Invalid())
            .arrange()
        advanceUntilIdle()
        viewModel.acceptClicked()
        viewModel.state.assertStateVisible { it.error shouldBeInstanceOf LegalHoldRequestedError.InvalidCredentialsError::class }
    }

    @Test
    fun givenInvalidPasswordResult_whenApproving_thenErrorStateShouldBeInvalidCredentials() = runTest {
        val (_, viewModel) = arrangeWithLegalHoldRequest()
            .withValidatePasswordResult(ValidatePasswordResult.Valid)
            .withApproveLegalHoldRequestResult(ApproveLegalHoldRequestUseCase.Result.Failure.InvalidPassword)
            .arrange()
        advanceUntilIdle()
        viewModel.acceptClicked()
        viewModel.state.assertStateVisible {
            it.error shouldBeInstanceOf LegalHoldRequestedError.InvalidCredentialsError::class
            it.requiresPassword shouldBeEqualTo true
        }
    }

    @Test
    fun givenPasswordRequiredResult_whenApproving_thenErrorStateShouldBeInvalidCredentialsAndRequiresPasswordTrue() = runTest {
        val (_, viewModel) = arrangeWithLegalHoldRequest(isPasswordRequired = false)
            .withValidatePasswordResult(ValidatePasswordResult.Valid)
            .withApproveLegalHoldRequestResult(ApproveLegalHoldRequestUseCase.Result.Failure.PasswordRequired)
            .arrange()
        advanceUntilIdle()
        viewModel.acceptClicked()
        viewModel.state.assertStateVisible {
            it.error shouldBeInstanceOf LegalHoldRequestedError.InvalidCredentialsError::class
            it.requiresPassword shouldBeEqualTo true
        }
    }

    @Test
    fun givenGenericFailureResult_whenApproving_thenErrorStateShouldBeGenericError() = runTest {
        val (_, viewModel) = arrangeWithLegalHoldRequest()
            .withValidatePasswordResult(ValidatePasswordResult.Valid)
            .withApproveLegalHoldRequestResult(ApproveLegalHoldRequestUseCase.Result.Failure.GenericFailure(UNKNOWN_ERROR))
            .arrange()
        advanceUntilIdle()
        viewModel.acceptClicked()
        viewModel.state.assertStateVisible {
            it.error shouldBeInstanceOf LegalHoldRequestedError.GenericError::class
        }
    }

    @Test
    fun givenSuccess_whenApproving_thenStateShouldBeHidden() = runTest {
        val (_, viewModel) = arrangeWithLegalHoldRequest()
            .withValidatePasswordResult(ValidatePasswordResult.Valid)
            .withApproveLegalHoldRequestResult(ApproveLegalHoldRequestUseCase.Result.Success)
            .arrange()
        advanceUntilIdle()
        viewModel.acceptClicked()
        viewModel.state shouldBeInstanceOf LegalHoldRequestedState.Hidden::class
    }

    @Test
    fun givenPasswordNotRequired_whenApproving_thenShouldNotValidatePasswordAndExecuteWithEmptyPassword() = runTest {
        val (arrangement, viewModel) = arrangeWithLegalHoldRequest(isPasswordRequired = false)
            .withApproveLegalHoldRequestResult(ApproveLegalHoldRequestUseCase.Result.Success)
            .arrange()
        advanceUntilIdle()
        viewModel.acceptClicked()
        verify(exactly = 0) { arrangement.validatePassword(any()) }
        coVerify { arrangement.userSessionScope.approveLegalHoldRequest(matchNullable { it == null }) }
    }

    @Test
    fun givenPasswordRequired_whenApproving_thenShouldValidatePasswordAndNotExecuteWithEmptyPassword() = runTest {
        val (arrangement, viewModel) = arrangeWithLegalHoldRequest(isPasswordRequired = true)
            .withValidatePasswordResult(ValidatePasswordResult.Invalid())
            .withApproveLegalHoldRequestResult(ApproveLegalHoldRequestUseCase.Result.Success)
            .arrange()
        val password = "invalidpassword"
        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd(password)
        advanceUntilIdle()
        viewModel.acceptClicked()
        verify { arrangement.validatePassword(password) }
        coVerify(exactly = 0) { arrangement.userSessionScope.approveLegalHoldRequest(matchNullable { it.isNullOrEmpty() }) }
    }

    @Test
    fun givenPasswordRequiredAndInvalidPassword_whenApproving_thenShouldNotExecuteWithInvalidPassword() = runTest {
        val (arrangement, viewModel) = arrangeWithLegalHoldRequest(isPasswordRequired = true)
            .withValidatePasswordResult(ValidatePasswordResult.Invalid())
            .withApproveLegalHoldRequestResult(ApproveLegalHoldRequestUseCase.Result.Success)
            .arrange()
        val password = "invalidpassword"
        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd(password)
        advanceUntilIdle()
        viewModel.acceptClicked()
        coVerify(exactly = 0) { arrangement.userSessionScope.approveLegalHoldRequest(password) }
    }

    @Test
    fun givenPasswordRequiredAndValidPassword_whenApproving_thenShouldExecuteWithValidPassword() = runTest {
        val (arrangement, viewModel) = arrangeWithLegalHoldRequest(isPasswordRequired = true)
            .withValidatePasswordResult(ValidatePasswordResult.Valid)
            .withApproveLegalHoldRequestResult(ApproveLegalHoldRequestUseCase.Result.Success)
            .arrange()
        val password = "ValidPassword123!"
        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd(password)
        advanceUntilIdle()
        viewModel.acceptClicked()
        coVerify { arrangement.userSessionScope.approveLegalHoldRequest(password) }
    }

    private class Arrangement {

        @MockK
        lateinit var validatePassword: ValidatePasswordUseCase

        @MockK
        lateinit var coreLogic: CoreLogic

        @MockK
        lateinit var userSessionScope: UserSessionScope

        val userId = UserId("userId", "domain")

        val viewModel by lazy {
            LegalHoldRequestedViewModel(
                validatePassword = validatePassword,
                coreLogic = { coreLogic },
            )
        }

        init {
            MockKAnnotations.init(this)
            every { coreLogic.getSessionScope(userId) } returns userSessionScope
        }
        fun withNotCurrentSession() = apply {
            every { coreLogic.globalScope { session.currentSessionFlow() } } returns
                    flowOf(CurrentSessionResult.Failure.SessionNotFound)
        }
        fun withCurrentSessionFailure() = apply {
            every { coreLogic.globalScope { session.currentSessionFlow() } } returns
                    flowOf(CurrentSessionResult.Failure.Generic(UNKNOWN_ERROR))
        }
        fun withCurrentSessionExists() = apply {
            every { coreLogic.globalScope { session.currentSessionFlow() } } returns
                    flowOf(CurrentSessionResult.Success(AccountInfo.Valid(userId)))
        }
        fun withLegalHoldRequestResult(result: ObserveLegalHoldRequestUseCase.Result) = apply {
            every { userSessionScope.observeLegalHoldRequest() } returns flowOf(result)
        }
        fun withIsPasswordRequiredResult(result: IsPasswordRequiredUseCase.Result) = apply {
            coEvery { userSessionScope.users.isPasswordRequired() } returns result
        }
        fun withValidatePasswordResult(result: ValidatePasswordResult) = apply {
            coEvery { validatePassword(any()) } returns result
        }
        fun withApproveLegalHoldRequestResult(result: ApproveLegalHoldRequestUseCase.Result) = apply {
            coEvery { userSessionScope.approveLegalHoldRequest(any()) } returns result
        }

        fun arrange() = this to viewModel.apply { observeLegalHoldRequest() }

        companion object {
            val UNKNOWN_ERROR = CoreFailure.Unknown(RuntimeException("error"))
        }
    }
}
