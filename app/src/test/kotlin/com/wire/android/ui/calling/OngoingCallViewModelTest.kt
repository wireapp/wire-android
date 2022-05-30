package com.wire.android.ui.calling

import androidx.lifecycle.SavedStateHandle
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.GetAllCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.MuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.SetVideoPreviewUseCase
import com.wire.kalium.logic.feature.call.usecase.UnMuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.UpdateVideoStateUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class OngoingCallViewModelTest {

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    private lateinit var navigationManager: NavigationManager

    @MockK
    private lateinit var allCalls: GetAllCallsUseCase

    @MockK
    private lateinit var endCall: EndCallUseCase

    @MockK
    private lateinit var muteCall: MuteCallUseCase

    @MockK
    private lateinit var unMuteCall: UnMuteCallUseCase

    @MockK
    private lateinit var observeConversationDetails: ObserveConversationDetailsUseCase

    @MockK
    private lateinit var setVideoPreview: SetVideoPreviewUseCase

    @MockK
    private lateinit var updateVideoState: UpdateVideoStateUseCase

    private lateinit var ongoingCallViewModel: OngoingCallViewModel

    @BeforeEach
    fun setup() {
        val scheduler = TestCoroutineScheduler()
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        val dummyConversationId = "some-dummy-value@some.dummy.domain"
        MockKAnnotations.init(this)
        every { savedStateHandle.get<String>(any()) } returns dummyConversationId
        every { savedStateHandle.set(any(), any<String>()) } returns Unit

        ongoingCallViewModel = OngoingCallViewModel(
            savedStateHandle = savedStateHandle,
            navigationManager = navigationManager,
            conversationDetails = observeConversationDetails,
            allCalls = allCalls,
            endCall = endCall,
            muteCall = muteCall,
            unMuteCall = unMuteCall,
            setVideoPreview = setVideoPreview,
            updateVideoState = updateVideoState
        )
    }

    @Test
    fun `given muteOrUnMuteCall is called, when active call is muted, then un-mute the call`() {
        ongoingCallViewModel.callEstablishedState = ongoingCallViewModel.callEstablishedState.copy(isMuted = false)
        coEvery { muteCall.invoke() } returns Unit

        runTest { ongoingCallViewModel.muteOrUnMuteCall() }

        coVerify(exactly = 1) { muteCall.invoke() }
        ongoingCallViewModel.callEstablishedState.isMuted shouldBeEqualTo true
    }

    @Test
    fun `given muteOrUnMuteCall is called, when active call is un-muted, then mute the call`() {
        ongoingCallViewModel.callEstablishedState = ongoingCallViewModel.callEstablishedState.copy(isMuted = true)
        coEvery { unMuteCall.invoke() } returns Unit

        runTest { ongoingCallViewModel.muteOrUnMuteCall() }

        coVerify(exactly = 1) { unMuteCall.invoke() }
        ongoingCallViewModel.callEstablishedState.isMuted shouldBeEqualTo false

    }

    @Test
    fun `given camera is turned on, when toggling video, then turn off video`() {
        ongoingCallViewModel.callEstablishedState = ongoingCallViewModel.callEstablishedState.copy(isCameraOn = true)
        coEvery { updateVideoState(any(), any()) } returns Unit

        runTest { ongoingCallViewModel.toggleVideo() }

        coVerify(exactly = 1) { updateVideoState(any(), any()) }
        ongoingCallViewModel.callEstablishedState.isCameraOn shouldBeEqualTo false
    }

    @Test
    fun `given camera is turned off, when toggling video, then turn on video`() {
        ongoingCallViewModel.callEstablishedState = ongoingCallViewModel.callEstablishedState.copy(isCameraOn = false)
        coEvery { updateVideoState(any(), any()) } returns Unit

        runTest { ongoingCallViewModel.toggleVideo() }

        coVerify(exactly = 1) { updateVideoState(any(), any()) }
        ongoingCallViewModel.callEstablishedState.isCameraOn shouldBeEqualTo true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}
