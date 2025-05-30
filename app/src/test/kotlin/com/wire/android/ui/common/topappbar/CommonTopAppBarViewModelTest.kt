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

package com.wire.android.ui.common.topappbar

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.GlobalKaliumScope
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.usecase.GetIncomingCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveOutgoingCallUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import com.wire.kalium.network.NetworkState
import com.wire.kalium.network.NetworkStateObserver
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
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class CommonTopAppBarViewModelTest {

    @Test
    fun givenNoCallAndHomeScreenAndSlowSync_whenGettingState_thenShouldHaveConnectingInfo() =
        runTest {
            val (_, commonTopAppBarViewModel) = Arrangement()
                .withCurrentSessionExist()
                .withoutOngoingCall()
                .withoutOutgoingCall()
                .withoutIncomingCall()
                .withCurrentScreen(CurrentScreen.Home)
                .withSyncState(SyncState.SlowSync)
                .arrange()

            advanceUntilIdle()
            val state = commonTopAppBarViewModel.state

            val info = state.connectivityState
            info shouldBeInstanceOf ConnectivityUIState.Connecting::class
        }

    @Test
    fun givenNoCallAndHomeScreenAndGathering_whenGettingState_thenShouldHaveConnectingInfo() =
        runTest {
            val (_, commonTopAppBarViewModel) = Arrangement()
                .withCurrentSessionExist()
                .withoutOngoingCall()
                .withoutOutgoingCall()
                .withoutIncomingCall()
                .withCurrentScreen(CurrentScreen.Home)
                .withSyncState(SyncState.GatheringPendingEvents)
                .arrange()

            advanceUntilIdle()
            val state = commonTopAppBarViewModel.state

            val info = state.connectivityState
            info shouldBeInstanceOf ConnectivityUIState.Connecting::class
        }

    @Test
    fun givenAnOngoingCallAndHomeScreenAndConnectivityIssues_whenGettingState_thenShouldHaveActiveCallInfo() =
        runTest {
            val (_, commonTopAppBarViewModel) = Arrangement()
                .withCurrentSessionExist()
                .withOngoingCall()
                .withoutOutgoingCall()
                .withoutIncomingCall()
                .withCurrentScreen(CurrentScreen.Home)
                .withSyncState(SyncState.Waiting)
                .arrange()

            advanceUntilIdle()
            val state = commonTopAppBarViewModel.state

            val info = state.connectivityState
            info.shouldBeInstanceOf<ConnectivityUIState.Calls>().let {
                it.calls.shouldHaveSize(1)
                it.calls[0].shouldBeInstanceOf<ConnectivityUIState.Call.Established>()
                    .conversationId shouldBeEqualTo ongoingCall.conversationId
            }
        }

    @Test
    fun givenAnOngoingCallAndCallIsMuted_whenGettingState_thenShouldHaveMutedCallInfo() = runTest {
        val (_, commonTopAppBarViewModel) = Arrangement()
            .withCurrentSessionExist()
            .withOngoingCall(isMuted = true)
            .withoutOutgoingCall()
            .withoutIncomingCall()
            .withCurrentScreen(CurrentScreen.Conversation(mockk()))
            .withSyncState(SyncState.Waiting)
            .arrange()

        advanceUntilIdle()
        val state = commonTopAppBarViewModel.state

        val info = state.connectivityState
        info.shouldBeInstanceOf<ConnectivityUIState.Calls>().let {
            it.calls.shouldHaveSize(1)
            it.calls[0].shouldBeInstanceOf<ConnectivityUIState.Call.Established>()
                .isMuted shouldBe true
        }
    }

    @Test
    fun givenAnOngoingCallAndCallIsNotMuted_whenGettingState_thenShouldNotHaveMutedCallInfo() =
        runTest {
            val (_, commonTopAppBarViewModel) = Arrangement()
                .withCurrentSessionExist()
                .withOngoingCall(isMuted = false)
                .withoutOutgoingCall()
                .withoutIncomingCall()
                .withCurrentScreen(CurrentScreen.Conversation(mockk()))
                .withSyncState(SyncState.Waiting)
                .arrange()

            advanceUntilIdle()
            val state = commonTopAppBarViewModel.state

            val info = state.connectivityState
            info.shouldBeInstanceOf<ConnectivityUIState.Calls>().let {
                it.calls.shouldHaveSize(1)
                it.calls[0].shouldBeInstanceOf<ConnectivityUIState.Call.Established>()
                    .isMuted shouldBe false
            }
        }

    @Test
    fun givenAnOngoingCallAndConnectivityIssueAndSomeOtherScreen_whenGettingState_thenShouldHaveActiveCallInfo() =
        runTest {
            val (_, commonTopAppBarViewModel) = Arrangement()
                .withCurrentSessionExist()
                .withOngoingCall(isMuted = false)
                .withoutOutgoingCall()
                .withoutIncomingCall()
                .withCurrentScreen(CurrentScreen.SomeOther())
                .withSyncState(SyncState.Waiting)
                .arrange()

            advanceUntilIdle()
            val state = commonTopAppBarViewModel.state

            val info = state.connectivityState
            info.shouldBeInstanceOf<ConnectivityUIState.Calls>().let {
                it.calls.shouldHaveSize(1)
                it.calls[0].shouldBeInstanceOf<ConnectivityUIState.Call.Established>()
            }
        }

    @Test
    fun givenAnIncomingCallAnd_whenGettingState_thenShouldHaveIncomingCallInfo() = runTest {
        val (_, commonTopAppBarViewModel) = Arrangement()
            .withCurrentSessionExist()
            .withIncomingCall()
            .withoutOngoingCall()
            .withoutOutgoingCall()
            .withCurrentScreen(CurrentScreen.Home)
            .withSyncState(SyncState.Waiting)
            .arrange()

        advanceUntilIdle()
        val state = commonTopAppBarViewModel.state

        val info = state.connectivityState
        info.shouldBeInstanceOf<ConnectivityUIState.Calls>().let {
            it.calls.shouldHaveSize(1)
            it.calls[0].shouldBeInstanceOf<ConnectivityUIState.Call.Incoming>()
        }
    }

    @Test
    fun givenAnOutgoingCallAnd_whenGettingState_thenShouldHaveOutgoingCallInfo() = runTest {
        val (_, commonTopAppBarViewModel) = Arrangement()
            .withCurrentSessionExist()
            .withOutgoingCall()
            .withoutIncomingCall()
            .withoutOngoingCall()
            .withCurrentScreen(CurrentScreen.Home)
            .withSyncState(SyncState.Live)
            .arrange()

        advanceUntilIdle()
        val state = commonTopAppBarViewModel.state

        val info = state.connectivityState
        info.shouldBeInstanceOf<ConnectivityUIState.Calls>().let {
            it.calls.shouldHaveSize(1)
            it.calls[0].shouldBeInstanceOf<ConnectivityUIState.Call.Outgoing>()
        }
    }

    @Test
    fun givenNoCurrentSession_whenGettingState_thenNone() = runTest {
        val (_, commonTopAppBarViewModel) = Arrangement()
            .withNotCurrentSession()
            .arrange()

        advanceUntilIdle()
        val state = commonTopAppBarViewModel.state

        state.connectivityState shouldBeInstanceOf ConnectivityUIState.None::class
    }

    @Test
    fun givenEstablishedAndIncomingCall_whenActiveCallFlowsIsCalled_thenEmitBoth() = runTest {
        val (_, commonTopAppBarViewModel) = Arrangement()
            .withCurrentSessionExist()
            .withOngoingCall(isMuted = true)
            .withIncomingCall()
            .withoutOutgoingCall()
            .withCurrentScreen(CurrentScreen.Home)
            .withSyncState(SyncState.Waiting)
            .arrange()

        val flow = commonTopAppBarViewModel.activeCallsFlow(userId)

        flow.collect {
            it shouldBeEqualTo listOf(ongoingCall, incomingCall)
        }
    }

    private class Arrangement {

        @MockK
        private lateinit var observeEstablishedCalls: ObserveEstablishedCallsUseCase

        @MockK
        private lateinit var incomingCalls: GetIncomingCallsUseCase

        @MockK
        private lateinit var observeOutgoingCall: ObserveOutgoingCallUseCase

        @MockK
        private lateinit var currentScreenManager: CurrentScreenManager

        @MockK
        private lateinit var observeSyncState: ObserveSyncStateUseCase

        @MockK
        private lateinit var coreLogic: CoreLogic

        @MockK
        private lateinit var globalKaliumScope: GlobalKaliumScope

        @MockK
        private lateinit var networkStateObserver: NetworkStateObserver

        init {
            MockKAnnotations.init(this)
            every {
                coreLogic.sessionScope(any()) {
                    observeSyncState
                }
            } returns observeSyncState

            every {
                coreLogic.sessionScope(any()) {
                    calls.establishedCall
                }
            } returns observeEstablishedCalls

            every {
                coreLogic.sessionScope(any()) {
                    calls.getIncomingCalls
                }
            } returns incomingCalls

            every {
                coreLogic.sessionScope(any()) {
                    calls.observeOutgoingCall
                }
            } returns observeOutgoingCall

            every {
                coreLogic.getGlobalScope()
            } returns globalKaliumScope

            every {
                coreLogic.networkStateObserver
            } returns networkStateObserver

            every {
                networkStateObserver.observeNetworkState()
            } returns MutableStateFlow(NetworkState.ConnectedWithInternet)

            every {
                globalKaliumScope.session.currentSessionFlow()
            } returns emptyFlow()

            withSyncState(SyncState.Live)
            withoutOngoingCall()
        }

        private val commonTopAppBarViewModel by lazy {
            CommonTopAppBarViewModel(
                currentScreenManager = currentScreenManager,
                coreLogic = { coreLogic },
            )
        }

        fun withOngoingCall(isMuted: Boolean = false) = apply {
            coEvery { observeEstablishedCalls() } returns flowOf(
                listOf(ongoingCall.copy(isMuted = isMuted))
            )
        }

        fun withoutOngoingCall() = apply {
            coEvery { observeEstablishedCalls() } returns flowOf(listOf())
        }

        fun withoutOutgoingCall() = apply {
            coEvery { observeOutgoingCall() } returns flowOf(listOf())
        }

        fun withOutgoingCall() = apply {
            coEvery { observeOutgoingCall() } returns flowOf(
                listOf(outgoingCall)
            )
        }

        fun withoutIncomingCall() = apply {
            coEvery { incomingCalls() } returns flowOf(listOf())
        }

        fun withIncomingCall() = apply {
            coEvery { incomingCalls() } returns flowOf(
                listOf(incomingCall)
            )
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
                    AccountInfo.Valid(userId)
                )
            )
        }

        fun withCurrentScreen(currentScreen: CurrentScreen) = apply {
            coEvery { currentScreenManager.observeCurrentScreen(any()) } returns MutableStateFlow(
                currentScreen
            )
        }

        fun arrange() = this to commonTopAppBarViewModel
    }

    companion object {
        val userId = UserId("userId", "domain")
        val conversationId = ConversationId("first", "domain")
        val ongoingCall = Call(
            conversationId,
            CallStatus.ESTABLISHED,
            true,
            false,
            false,
            UserId("caller", "domain"),
            "ONE_ON_ONE Name",
            Conversation.Type.OneOnOne,
            "otherUsername",
            "team1"
        )
        val outgoingCall = ongoingCall.copy(
            status = CallStatus.STARTED
        )
        val incomingCall = ongoingCall.copy(
            status = CallStatus.INCOMING
        )
    }
}
