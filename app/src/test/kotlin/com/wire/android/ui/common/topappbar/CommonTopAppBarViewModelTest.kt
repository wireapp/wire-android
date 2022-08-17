package com.wire.android.ui.common.topappbar

import com.wire.android.navigation.NavigationManager
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.feature.call.Call
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CommonTopAppBarViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun givenNoActiveCallAndHomeScreenAndSlowSync_whenGettingState_thenShouldHaveConnectingInfo() = runTest(dispatcher) {
        val (_, commonTopAppBarViewModel) = Arrangement()
            .withoutActiveCall()
            .withCurrentScreen(CurrentScreen.Home)
            .withSyncState(SyncState.SlowSync)
            .arrange()

        advanceUntilIdle()
        val state = commonTopAppBarViewModel.callState

        val info = state.info
        info shouldBeInstanceOf ConnectivityUIState.Info.Connecting::class
    }

    @Test
    fun givenNoActiveCallAndHomeScreenAndGathering_whenGettingState_thenShouldHaveConnectingInfo() = runTest(dispatcher) {
        val (_, commonTopAppBarViewModel) = Arrangement()
            .withoutActiveCall()
            .withCurrentScreen(CurrentScreen.Home)
            .withSyncState(SyncState.GatheringPendingEvents)
            .arrange()

        advanceUntilIdle()
        val state = commonTopAppBarViewModel.callState

        val info = state.info
        info shouldBeInstanceOf ConnectivityUIState.Info.Connecting::class
    }

    @Test
    fun givenActiveCallAndHomeScreenAndConnectivityIssues_whenGettingState_thenShouldHaveActiveCallInfo() = runTest(dispatcher) {
        val (arrangement, commonTopAppBarViewModel) = Arrangement()
            .withActiveCall()
            .withCurrentScreen(CurrentScreen.Home)
            .withSyncState(SyncState.Waiting)
            .arrange()

        advanceUntilIdle()
        val state = commonTopAppBarViewModel.callState

        val info = state.info
        info shouldBeInstanceOf ConnectivityUIState.Info.EstablishedCall::class
        info as ConnectivityUIState.Info.EstablishedCall
        info.conversationId shouldBeEqualTo arrangement.activeCall.conversationId
    }

    @Test
    fun givenActiveCallAndCallScreenAndConnectivityIssues_whenGettingState_thenShouldHaveConnectivityInfo() = runTest(dispatcher) {
        val (_, commonTopAppBarViewModel) = Arrangement()
            .withActiveCall()
            .withCurrentScreen(CurrentScreen.OngoingCallScreen(mockk()))
            .withSyncState(SyncState.Waiting)
            .arrange()

        advanceUntilIdle()
        val state = commonTopAppBarViewModel.callState

        val info = state.info
        info shouldBeInstanceOf ConnectivityUIState.Info.WaitingConnection::class
    }

    @Test
    fun givenActiveCallAndCallIsMuted_whenGettingState_thenShouldHaveMutedCallInfo() = runTest(dispatcher) {
        val (_, commonTopAppBarViewModel) = Arrangement()
            .withActiveCall()
            .withCurrentScreen(CurrentScreen.Conversation(mockk()))
            .withCallMuted(true)
            .withSyncState(SyncState.Waiting)
            .arrange()

        advanceUntilIdle()
        val state = commonTopAppBarViewModel.callState

        val info = state.info
        info shouldBeInstanceOf ConnectivityUIState.Info.EstablishedCall::class
        info as ConnectivityUIState.Info.EstablishedCall
        info.isMuted shouldBe true
    }

    @Test
    fun givenActiveCallAndCallIsNotMuted_whenGettingState_thenShouldNotHaveMutedCallInfo() = runTest(dispatcher) {
        val (_, commonTopAppBarViewModel) = Arrangement()
            .withActiveCall()
            .withCurrentScreen(CurrentScreen.Conversation(mockk()))
            .withCallMuted(false)
            .withSyncState(SyncState.Waiting)
            .arrange()

        advanceUntilIdle()
        val state = commonTopAppBarViewModel.callState

        val info = state.info
        info shouldBeInstanceOf ConnectivityUIState.Info.EstablishedCall::class
        info as ConnectivityUIState.Info.EstablishedCall
        info.isMuted shouldBe false
    }

    @Test
    fun givenActiveCallAndConnectivityIssueAndSomeOtherScreen_whenGettingState_thenShouldHaveNoInfo() = runTest(dispatcher) {
        val (_, commonTopAppBarViewModel) = Arrangement()
            .withActiveCall()
            .withCurrentScreen(CurrentScreen.SomeOther)
            .withSyncState(SyncState.Waiting)
            .arrange()

        advanceUntilIdle()
        val state = commonTopAppBarViewModel.callState

        val info = state.info
        info shouldBeInstanceOf ConnectivityUIState.Info.None::class
    }

    private class Arrangement {

        val activeCall: Call = mockk()
        val conversationId: ConversationId = mockk()

        private var isCallMuted = true

        @MockK
        private lateinit var navigationManager: NavigationManager

        @MockK
        private lateinit var establishedCalls: ObserveEstablishedCallsUseCase

        @MockK
        private lateinit var currentScreenManager: CurrentScreenManager

        @MockK
        private lateinit var observeSyncState: ObserveSyncStateUseCase

        init {
            MockKAnnotations.init(this)
            every { activeCall.conversationId } returns conversationId
        }

        private val commonTopAppBarViewModel by lazy {
            every { activeCall.isMuted } returns isCallMuted
            CommonTopAppBarViewModel(
                navigationManager,
                establishedCalls,
                currentScreenManager,
                observeSyncState
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

        fun withCurrentScreen(currentScreen: CurrentScreen) = apply {
            coEvery { currentScreenManager.observeCurrentScreen(any()) } returns MutableStateFlow(currentScreen)
        }

        fun arrange() = this to commonTopAppBarViewModel

    }
}
