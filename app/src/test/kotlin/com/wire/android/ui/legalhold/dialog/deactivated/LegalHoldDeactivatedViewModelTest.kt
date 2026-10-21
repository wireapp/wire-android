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
package com.wire.android.ui.legalhold.dialog.deactivated

import com.wire.android.session.FakeSessionAdmission
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.ui.legalhold.dialog.deactivated.LegalHoldDeactivatedViewModelTest.Arrangement.Companion.UNKNOWN_ERROR
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.UserSessionScope
import io.mockk.mockk
import io.mockk.verify
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.legalhold.LegalHoldState
import com.wire.kalium.logic.feature.legalhold.MarkLegalHoldChangeAsNotifiedForSelfUseCase
import com.wire.kalium.logic.feature.legalhold.ObserveLegalHoldChangeNotifiedForSelfUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runCurrent
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel
import org.junit.jupiter.api.Assertions.assertTrue
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import com.wire.android.assertions.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class LegalHoldDeactivatedViewModelTest {

    @Test
    fun givenNoSession_whenGettingState_thenStateShouldBeHidden() = runTest {
        val (_, viewModel) = Arrangement()
            .withNotCurrentSession()
            .arrange()
        advanceUntilIdle()
        viewModel.state shouldBeInstanceOf LegalHoldDeactivatedState.Hidden::class
    }

    @Test
    fun givenSessionReturnsFailure_whenGettingState_thenStateShouldBeHidden() = runTest {
        val (_, viewModel) = Arrangement()
            .withCurrentSessionFailure()
            .arrange()
        advanceUntilIdle()
        viewModel.state shouldBeInstanceOf LegalHoldDeactivatedState.Hidden::class
    }

    @Test
    fun givenLegalHoldRequestReturnsFailure_whenGettingState_thenStateShouldBeHidden() = runTest {
        val (_, viewModel) = Arrangement()
            .withCurrentSessionExists()
            .withLegalHoldChangeNotifiedResult(ObserveLegalHoldChangeNotifiedForSelfUseCase.Result.Failure(UNKNOWN_ERROR))
            .arrange()
        advanceUntilIdle()
        viewModel.state shouldBeInstanceOf LegalHoldDeactivatedState.Hidden::class
    }

    @Test
    fun givenAlreadyNotified_whenGettingState_thenStateShouldBeHidden() = runTest {
        val (_, viewModel) = Arrangement()
            .withCurrentSessionExists()
            .withLegalHoldChangeNotifiedResult(ObserveLegalHoldChangeNotifiedForSelfUseCase.Result.AlreadyNotified)
            .arrange()
        advanceUntilIdle()
        viewModel.state shouldBeInstanceOf LegalHoldDeactivatedState.Hidden::class
    }

    @Test
    fun givenShouldNotify_whenGettingState_thenStateShouldBeVisible() = runTest {
        val legalHoldState = LegalHoldState.Disabled
        val (_, viewModel) = Arrangement()
            .withCurrentSessionExists()
            .withLegalHoldChangeNotifiedResult(ObserveLegalHoldChangeNotifiedForSelfUseCase.Result.ShouldNotify(legalHoldState))
            .arrange()
        advanceUntilIdle()
        viewModel.state shouldBeInstanceOf LegalHoldDeactivatedState.Visible::class
    }

    @Test
    fun givenShouldNotify_whenDismissing_thenStateShouldBeChangedToHidden() = runTest {
        val legalHoldState = LegalHoldState.Disabled
        val (arrangement, viewModel) = Arrangement()
            .withCurrentSessionExists()
            .withLegalHoldChangeNotifiedResult(ObserveLegalHoldChangeNotifiedForSelfUseCase.Result.ShouldNotify(legalHoldState))
            .withMarkLegalHoldChangeAsNotifiedResult(MarkLegalHoldChangeAsNotifiedForSelfUseCase.Result.Success)
            .arrange()
        advanceUntilIdle()
        viewModel.dismiss()
        advanceUntilIdle()
        viewModel.state shouldBeInstanceOf LegalHoldDeactivatedState.Hidden::class
        coVerify { arrangement.session.markLegalHoldChangeAsNotifiedForSelf() }
    }

    @Test
    fun `enabled notification action stays in its admission and drains before replacement`() = runTest {
        val arrangement = Arrangement().withCurrentSessionExists()
            .withLegalHoldChangeNotifiedResult(
                ObserveLegalHoldChangeNotifiedForSelfUseCase.Result.ShouldNotify(LegalHoldState.Enabled)
            )
        val entered = CompletableDeferred<Unit>()
        val drained = CompletableDeferred<Unit>()
        coEvery { arrangement.session.markLegalHoldChangeAsNotifiedForSelf() } coAnswers {
            entered.complete(Unit)
            try { awaitCancellation() } finally { drained.complete(Unit) }
        }
        arrangement.admission.publish(arrangement.userId, null)
        val (_, viewModel) = arrangement.arrange()
        runCurrent()
        coVerify(exactly = 0) { arrangement.session.markLegalHoldChangeAsNotifiedForSelf() }
        arrangement.admission.publish(arrangement.userId, arrangement.session)
        entered.await()
        arrangement.admission.publish(arrangement.userId, null)
        runCurrent()
        assertTrue(drained.isCompleted)
        arrangement.withLegalHoldChangeNotifiedResult(
            ObserveLegalHoldChangeNotifiedForSelfUseCase.Result.ShouldNotify(LegalHoldState.Disabled)
        )
        arrangement.admission.publish(arrangement.userId, arrangement.session)
        runCurrent()
        viewModel.state shouldBeInstanceOf LegalHoldDeactivatedState.Visible::class
        coVerify(exactly = 1) { arrangement.session.markLegalHoldChangeAsNotifiedForSelf() }
        verify(exactly = 0) { arrangement.coreLogic.getSessionScope(any()) }
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `stale visible dismissal cannot act after raw account invalidation`() = runTest {
        val (arrangement, viewModel) = Arrangement().withCurrentSessionExists()
            .withLegalHoldChangeNotifiedResult(
                ObserveLegalHoldChangeNotifiedForSelfUseCase.Result.ShouldNotify(LegalHoldState.Disabled)
            ).arrange()
        viewModel.state shouldBeInstanceOf LegalHoldDeactivatedState.Visible::class
        arrangement.rawAccounts.value = GetAllSessionsResult.Success(emptyList())
        viewModel.dismiss()
        arrangement.admission.publish(arrangement.userId, null)
        runCurrent()
        arrangement.admission.publish(arrangement.userId, arrangement.session)
        runCurrent()
        coVerify(exactly = 0) { arrangement.session.markLegalHoldChangeAsNotifiedForSelf() }
        viewModel.viewModelScope.cancel()
    }

    private class Arrangement {

        @MockK
        lateinit var coreLogic: CoreLogic
        val userId = UserId("userId", "domain")
        val session = mockk<UserSessionScope>()
        val rawAccounts = MutableStateFlow<GetAllSessionsResult>(GetAllSessionsResult.Success(listOf(AccountInfo.Valid(userId))))
        val admission by lazy { FakeSessionAdmission(coreLogic) { session } }
        val viewModel by lazy { LegalHoldDeactivatedViewModel(coreLogic = lazyOf(coreLogic)) }

        init {
            MockKAnnotations.init(this)
            admission
            coEvery { coreLogic.getGlobalScope().observeAllValidSessionsFlow() } returns rawAccounts
        }
        fun withNotCurrentSession() = apply {
            every { coreLogic.getGlobalScope().session.currentSessionFlow() } returns
                    flowOf(CurrentSessionResult.Failure.SessionNotFound)
        }
        fun withCurrentSessionFailure() = apply {
            every { coreLogic.getGlobalScope().session.currentSessionFlow() } returns
                    flowOf(CurrentSessionResult.Failure.Generic(UNKNOWN_ERROR))
        }
        fun withCurrentSessionExists() = apply {
            every { coreLogic.getGlobalScope().session.currentSessionFlow() } returns
                    flowOf(CurrentSessionResult.Success(AccountInfo.Valid(UserId("userId", "domain"))))
        }
        fun withLegalHoldChangeNotifiedResult(result: ObserveLegalHoldChangeNotifiedForSelfUseCase.Result) = apply {
            coEvery { session.observeLegalHoldChangeNotifiedForSelf() } returns flowOf(result)
        }
        fun withMarkLegalHoldChangeAsNotifiedResult(result: MarkLegalHoldChangeAsNotifiedForSelfUseCase.Result) = apply {
            coEvery { session.markLegalHoldChangeAsNotifiedForSelf() } returns result
        }
        fun arrange() = this to viewModel

        companion object {
            val UNKNOWN_ERROR = CoreFailure.Unknown(RuntimeException("error"))
        }
    }
}
