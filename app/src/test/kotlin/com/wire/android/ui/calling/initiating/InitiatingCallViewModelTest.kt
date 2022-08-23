package com.wire.android.ui.calling.initiating

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.media.CallRinger
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.mockConversationDetailsGroup
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.IsLastCallClosedUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.StartCallUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase.Result
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InitiatingCallViewModelTest {

    @Test
    fun `given an outgoing call, when the user ends call, then invoke endCall useCase and close the screen`() = runTest {
        // Given
        val (arrangement, viewModel) = Arrangement()
            .withEndingCall()
            .arrange()

        // When
        viewModel.hangUpCall()

        // Then
        with(arrangement) {
            coVerify(exactly = 1) { endCall(any()) }
            coVerify(exactly = 1) { callRinger.stop() }
            coVerify(exactly = 1) { navigationManager.navigateBack() }
        }
    }

    @Test
    fun `given no internet connection, when user tries to start a call, an info dialog is shown `() = runTest {
        // Given
        val (arrangement, viewModel) = Arrangement()
            .withNoInternetConnection()
            .arrange()

        // When
        viewModel.initiateCall()

        // Then
        with(arrangement) {
            coVerify(exactly = 0) { callRinger.stop() }
            coVerify(exactly = 0) { viewModel.navigateBack() }
            assertEquals(InitiatingCallState.InitiatingCallError.NoConnection, viewModel.initiatingCallState.error)
        }
    }

    @Test
    fun `given a no internet connection dialog, when user clicks on OK, dialog is dismissed and closes the current screen `() = runTest {
        // Given
        val (_, viewModel) = Arrangement()
            .withNoInternetConnectionDialogDisplayed()
            .arrange()

        // When
        viewModel.onDismissErrorDialog()

        // Then
        coVerify(exactly = 1) { viewModel.navigateBack() }
        assertEquals(InitiatingCallState.InitiatingCallError.None, viewModel.initiatingCallState.error)
    }

    private class Arrangement {

        @MockK
        private lateinit var savedStateHandle: SavedStateHandle

        @MockK
        private lateinit var establishedCalls: ObserveEstablishedCallsUseCase

        @MockK
        private lateinit var isLastCallClosed: IsLastCallClosedUseCase

        @MockK
        private lateinit var startCall: StartCallUseCase

        @MockK
        private lateinit var observeConversationDetailsUseCase: ObserveConversationDetailsUseCase

        @MockK
        private lateinit var qualifiedIdMapper: QualifiedIdMapper

        @MockK
        lateinit var callRinger: CallRinger

        @MockK
        lateinit var endCall: EndCallUseCase

        @MockK
        lateinit var navigationManager: NavigationManager

        val initiatingCallViewModel by lazy {
            InitiatingCallViewModel(
                savedStateHandle = savedStateHandle,
                navigationManager = navigationManager,
                conversationDetails = observeConversationDetailsUseCase,
                observeEstablishedCalls = establishedCalls,
                startCall = startCall,
                endCall = endCall,
                isLastCallClosed = isLastCallClosed,
                callRinger = callRinger,
                qualifiedIdMapper = qualifiedIdMapper,
                dispatchers = TestDispatcherProvider(),
            )
        }

        init {
            Dispatchers.setMain(StandardTestDispatcher(TestCoroutineScheduler()))
            val dummyConversationId = ConversationId("some-dummy-value", "some.dummy.domain")
            val dummyConversationDetails = mockConversationDetailsGroup("Some Mocked Group", dummyConversationId)
            MockKAnnotations.init(this)
            every { savedStateHandle.get<String>(any()) } returns "${dummyConversationId.value}@${dummyConversationId.domain}"
            every { savedStateHandle.set(any(), any<String>()) } returns Unit
            every {
                qualifiedIdMapper.fromStringToQualifiedID("some-dummy-value@some.dummy.domain")
            } returns QualifiedID("some-dummy-value", "some.dummy.domain")
            coEvery { navigationManager.navigateBack(any()) } returns Unit
            coEvery { establishedCalls() } returns flowOf(emptyList())
            coEvery { observeConversationDetailsUseCase(any()) } returns flowOf(Result.Success(dummyConversationDetails))
        }

        fun withEndingCall(): Arrangement = apply {
            coEvery { endCall(any()) } returns Unit
            every { callRinger.stop() } returns Unit
            coEvery { navigationManager.navigateBack() } returns Unit
        }

        fun withNoInternetConnection(): Arrangement = apply {
            coEvery { startCall(any(), any(), any(), any()) } returns StartCallUseCase.Result.SyncFailure
            every { callRinger.stop() } returns Unit
            coEvery { navigationManager.navigateBack() } returns Unit
        }

        fun withNoInternetConnectionDialogDisplayed(): Arrangement = apply {
            initiatingCallViewModel.initiatingCallState =
                initiatingCallViewModel.initiatingCallState.copy(error = InitiatingCallState.InitiatingCallError.NoConnection)
            coEvery { navigationManager.navigateBack() } returns Unit
        }

        fun arrange() = this to initiatingCallViewModel
    }
}
