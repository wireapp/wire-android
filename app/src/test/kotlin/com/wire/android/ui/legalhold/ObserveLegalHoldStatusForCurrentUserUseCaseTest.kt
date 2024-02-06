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
package com.wire.android.ui.legalhold

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.TestUser
import com.wire.android.ui.legalhold.banner.LegalHoldUIState
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.feature.legalhold.LegalHoldState
import com.wire.kalium.logic.feature.legalhold.ObserveLegalHoldRequestUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class ObserveLegalHoldStatusForCurrentUserUseCaseTest {

    @Test
    fun `given legal hold request available, then isUnderLegalHold is pending`() = runTest {
        // given
        val (_, useCase) = Arrangement()
            .withLegalHold(LegalHoldState.Enabled)
            .withLegalHoldRequest(ObserveLegalHoldRequestUseCase.Result.LegalHoldRequestAvailable("fingerprint".toByteArray()))
            .arrange()
        // then
        assertEquals(LegalHoldUIState.Pending, useCase.invoke().first())
    }
    @Test
    fun `given legal hold enabled, then isUnderLegalHold is active`() = runTest {
        // given
        val (_, useCase) = Arrangement()
            .withLegalHold(LegalHoldState.Enabled)
            .withLegalHoldRequest(ObserveLegalHoldRequestUseCase.Result.NoLegalHoldRequest)
            .arrange()
        // then
        assertEquals(LegalHoldUIState.Active, useCase.invoke().first())
    }
    @Test
    fun `given legal hold disabled and no request available, then isUnderLegalHold is none`() = runTest {
        // given
        val (_, useCase) = Arrangement()
            .withLegalHold(LegalHoldState.Disabled)
            .withLegalHoldRequest(ObserveLegalHoldRequestUseCase.Result.NoLegalHoldRequest)
            .arrange()
        // then
        assertEquals(LegalHoldUIState.None, useCase.invoke().first())
    }
    @Test
    fun `given no session, then isUnderLegalHold is none`() = runTest {
        // given
        val (_, useCase) = Arrangement()
            .withCurrentSession(CurrentSessionResult.Failure.SessionNotFound)
            .withLegalHold(LegalHoldState.Disabled)
            .withLegalHoldRequest(ObserveLegalHoldRequestUseCase.Result.NoLegalHoldRequest)
            .arrange()
        // then
        assertEquals(LegalHoldUIState.None, useCase.invoke().first())
    }
    @Test
    fun `given current session failure, then isUnderLegalHold is none`() = runTest {
        // given
        val (_, useCase) = Arrangement()
            .withCurrentSession(CurrentSessionResult.Failure.Generic(CoreFailure.Unknown(null)))
            .withLegalHold(LegalHoldState.Disabled)
            .withLegalHoldRequest(ObserveLegalHoldRequestUseCase.Result.NoLegalHoldRequest)
            .arrange()
        // then
        assertEquals(LegalHoldUIState.None, useCase.invoke().first())
    }

    internal class Arrangement {

        @MockK
        private lateinit var coreLogic: CoreLogic

        private val useCase by lazy { ObserveLegalHoldStatusForCurrentUserUseCase(coreLogic, TestDispatcherProvider()) }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            withCurrentSession(CurrentSessionResult.Success(AccountInfo.Valid(TestUser.USER_ID)))
        }
        fun arrange() = this to useCase
        fun withCurrentSession(result: CurrentSessionResult) = apply {
            coEvery { coreLogic.getGlobalScope().session.currentSessionFlow.invoke() } returns flowOf(result)
        }
        fun withLegalHoldRequest(result: ObserveLegalHoldRequestUseCase.Result) = apply {
            coEvery { coreLogic.getSessionScope(any()).observeLegalHoldRequest.invoke() } returns flowOf(result)
        }
        fun withLegalHold(result: LegalHoldState) = apply {
            coEvery { coreLogic.getSessionScope(any()).observeLegalHoldForSelfUser.invoke() } returns flowOf(result)
        }
    }
}
