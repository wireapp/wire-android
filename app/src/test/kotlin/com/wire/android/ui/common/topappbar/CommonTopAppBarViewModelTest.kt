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
 *
 *
 */

package com.wire.android.ui.common.topappbar

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.ui.legalhold.banner.LegalHoldUIState
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.GlobalKaliumScope
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.legalhold.ObserveLegalHoldRequestUseCaseResult
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class CommonTopAppBarViewModelTest {

    @Test
    fun givenNoActiveCallAndHomeScreenAndSlowSync_whenGettingState_thenShouldHaveConnectingInfo() = runTest {
        val (_, commonTopAppBarViewModel) = Arrangement()
            .withCurrentSessionExist()
            .withoutActiveCall()
            .withCurrentScreen(CurrentScreen.Home)
            .withSyncState(SyncState.SlowSync)
            .arrange()

        advanceUntilIdle()
        val state = commonTopAppBarViewModel.state

        val info = state.connectivityState
        info shouldBeInstanceOf ConnectivityUIState.Connecting::class
    }

    @Test
    fun givenNoActiveCallAndHomeScreenAndGathering_whenGettingState_thenShouldHaveConnectingInfo() = runTest {
        val (_, commonTopAppBarViewModel) = Arrangement()
            .withCurrentSessionExist()
            .withoutActiveCall()
            .withCurrentScreen(CurrentScreen.Home)
            .withSyncState(SyncState.GatheringPendingEvents)
            .arrange()

        advanceUntilIdle()
        val state = commonTopAppBarViewModel.state

        val info = state.connectivityState
        info shouldBeInstanceOf ConnectivityUIState.Connecting::class
    }

    @Test
    fun givenActiveCallAndHomeScreenAndConnectivityIssues_whenGettingState_thenShouldHaveActiveCallInfo() = runTest {
        val (arrangement, commonTopAppBarViewModel) = Arrangement()
            .withCurrentSessionExist()
            .withActiveCall()
            .withCurrentScreen(CurrentScreen.Home)
            .withSyncState(SyncState.Waiting)
            .arrange()

        advanceUntilIdle()
        val state = commonTopAppBarViewModel.state

        val info = state.connectivityState
        info shouldBeInstanceOf ConnectivityUIState.EstablishedCall::class
        info as ConnectivityUIState.EstablishedCall
        info.conversationId shouldBeEqualTo arrangement.activeCall.conversationId
    }

    @Test
    fun givenActiveCallAndCallScreenAndConnectivityIssues_whenGettingState_thenShouldHaveConnectivityInfo() = runTest {
        val (_, commonTopAppBarViewModel) = Arrangement()
            .withCurrentSessionExist()
            .withActiveCall()
            .withCurrentScreen(CurrentScreen.OngoingCallScreen(mockk()))
            .withSyncState(SyncState.Waiting)
            .arrange()

        advanceUntilIdle()
        val state = commonTopAppBarViewModel.state

        val info = state.connectivityState
        info shouldBeInstanceOf ConnectivityUIState.WaitingConnection::class
    }

    @Test
    fun givenActiveCallAndCallIsMuted_whenGettingState_thenShouldHaveMutedCallInfo() = runTest {
        val (_, commonTopAppBarViewModel) = Arrangement()
            .withCurrentSessionExist()
            .withActiveCall()
            .withCurrentScreen(CurrentScreen.Conversation(mockk()))
            .withCallMuted(true)
            .withSyncState(SyncState.Waiting)
            .arrange()

        advanceUntilIdle()
        val state = commonTopAppBarViewModel.state

        val info = state.connectivityState
        info shouldBeInstanceOf ConnectivityUIState.EstablishedCall::class
        info as ConnectivityUIState.EstablishedCall
        info.isMuted shouldBe true
    }

    @Test
    fun givenActiveCallAndCallIsNotMuted_whenGettingState_thenShouldNotHaveMutedCallInfo() = runTest {
        val (_, commonTopAppBarViewModel) = Arrangement()
            .withCurrentSessionExist()
            .withActiveCall()
            .withCurrentScreen(CurrentScreen.Conversation(mockk()))
            .withCallMuted(false)
            .withSyncState(SyncState.Waiting)
            .arrange()

        advanceUntilIdle()
        val state = commonTopAppBarViewModel.state

        val info = state.connectivityState
        info shouldBeInstanceOf ConnectivityUIState.EstablishedCall::class
        info as ConnectivityUIState.EstablishedCall
        info.isMuted shouldBe false
    }

    @Test
    fun givenActiveCallAndConnectivityIssueAndSomeOtherScreen_whenGettingState_thenShouldHaveActiveCallInfo() = runTest {
        val (_, commonTopAppBarViewModel) = Arrangement()
            .withCurrentSessionExist()
            .withActiveCall()
            .withCurrentScreen(CurrentScreen.SomeOther)
            .withCallMuted(false)
            .withSyncState(SyncState.Waiting)
            .arrange()

        advanceUntilIdle()
        val state = commonTopAppBarViewModel.state

        val info = state.connectivityState
        info shouldBeInstanceOf ConnectivityUIState.EstablishedCall::class
    }

    @Test
    fun givenNoCurrentSession_whenGettingState_thenNone() = runTest {
        val (_, commonTopAppBarViewModel) = Arrangement()
            .withNotCurrentSession()
            .arrange()

        advanceUntilIdle()
        val state = commonTopAppBarViewModel.state

        state.connectivityState shouldBeInstanceOf ConnectivityUIState.None::class
        state.legalHoldState shouldBeInstanceOf LegalHoldUIState.None::class
    }

    private fun testLegalHoldRequestInfo(
        currentScreen: CurrentScreen,
        result: ObserveLegalHoldRequestUseCaseResult,
        expectedState: LegalHoldUIState,
    ) = runTest {
        val (_, commonTopAppBarViewModel) = Arrangement()
            .withCurrentSessionExist()
            .withCurrentScreen(currentScreen)
            .withLegalHoldRequestResult(result)
            .arrange()

        advanceUntilIdle()
        val state = commonTopAppBarViewModel.state

        state.legalHoldState shouldBeInstanceOf expectedState::class
    }

    @Test
    fun givenNoLegalHoldRequest_whenGettingState_thenShouldNotHaveLegalHoldRequestInfo() = testLegalHoldRequestInfo(
        currentScreen = CurrentScreen.Home,
        result = ObserveLegalHoldRequestUseCaseResult.NoObserveLegalHoldRequest,
        expectedState = LegalHoldUIState.None
    )

    @Test
    fun givenLegalHoldRequestAndHomeScreen_whenGettingState_thenShouldHaveLegalHoldRequestInfo() = testLegalHoldRequestInfo(
        currentScreen = CurrentScreen.Home,
        result = ObserveLegalHoldRequestUseCaseResult.ObserveLegalHoldRequestAvailable(byteArrayOf()),
        expectedState = LegalHoldUIState.Pending
    )

    @Test
    fun givenLegalHoldRequestAndCallScreen_whenGettingState_thenShouldHaveLegalHoldRequestInfo() = testLegalHoldRequestInfo(
        currentScreen = CurrentScreen.OngoingCallScreen(mockk()),
        result = ObserveLegalHoldRequestUseCaseResult.ObserveLegalHoldRequestAvailable(byteArrayOf()),
        expectedState = LegalHoldUIState.Pending
    )

    @Test
    fun givenLegalHoldRequestAndAuthRelatedScreen_whenGettingState_thenShouldNotHaveLegalHoldRequestInfo() = testLegalHoldRequestInfo(
        currentScreen = CurrentScreen.AuthRelated,
        result = ObserveLegalHoldRequestUseCaseResult.ObserveLegalHoldRequestAvailable(byteArrayOf()),
        expectedState = LegalHoldUIState.None
    )

    private class Arrangement {

        val activeCall: Call = mockk()
        val conversationId: ConversationId = mockk()

        private var isCallMuted = true

        @MockK
        private lateinit var establishedCalls: ObserveEstablishedCallsUseCase

        @MockK
        private lateinit var currentScreenManager: CurrentScreenManager

        @MockK
        private lateinit var observeSyncState: ObserveSyncStateUseCase

        @MockK
        private lateinit var coreLogic: CoreLogic

        @MockK
        private lateinit var globalKaliumScope: GlobalKaliumScope

        init {
            MockKAnnotations.init(this)
            every { activeCall.conversationId } returns conversationId
            every {
                coreLogic.sessionScope(any()) {
                    observeSyncState
                }
            } returns observeSyncState

            every {
                coreLogic.sessionScope(any()) {
                    calls.establishedCall
                }
            } returns establishedCalls

            every {
                coreLogic.getGlobalScope()
            } returns globalKaliumScope

            every {
                globalKaliumScope.session.currentSessionFlow()
            } returns emptyFlow()

            withSyncState(SyncState.Live)
            withoutActiveCall()
            withLegalHoldRequestResult(ObserveLegalHoldRequestUseCaseResult.NoObserveLegalHoldRequest)
        }

        private val commonTopAppBarViewModel by lazy {
            every { activeCall.isMuted } returns isCallMuted
            CommonTopAppBarViewModel(
                currentScreenManager,
                coreLogic
            )
        }

        fun withCallMuted(isMuted: Boolean) = apply {
            isCallMuted = isMuted
        }

        fun withActiveCall() = apply {
            coEvery { establishedCalls() } returns flowOf(listOf(activeCall))
        }

        fun withoutActiveCall() = apply {
            coEvery { establishedCalls() } returns flowOf(listOf())
        }

        fun withSyncState(syncState: SyncState) = apply {
            every { observeSyncState() } returns flowOf(syncState)
        }

        fun withNotCurrentSession() = apply {
            every { coreLogic.globalScope { session.currentSessionFlow() } } returns flowOf(
                CurrentSessionResult.Failure.SessionNotFound
            )
        }

        fun withCurrentSessionExist() = apply {
            every { coreLogic.globalScope { session.currentSessionFlow() } } returns flowOf(
                CurrentSessionResult.Success(
                    AccountInfo.Valid(UserId("userId", "domain"))
                )
            )
        }

        fun withCurrentScreen(currentScreen: CurrentScreen) = apply {
            coEvery { currentScreenManager.observeCurrentScreen(any()) } returns MutableStateFlow(currentScreen)
        }

        fun withLegalHoldRequestResult(result: ObserveLegalHoldRequestUseCaseResult) = apply {
            every { coreLogic.getSessionScope(any()).observeLegalHoldRequest() } returns flowOf(result)
        }

        fun arrange() = this to commonTopAppBarViewModel
    }
}
