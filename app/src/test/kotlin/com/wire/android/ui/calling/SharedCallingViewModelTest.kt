package com.wire.android.ui.calling

import androidx.lifecycle.SavedStateHandle
import com.wire.android.media.CallRinger
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.GetAllCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.MuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.SetVideoPreviewUseCase
import com.wire.kalium.logic.feature.call.usecase.UnMuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.UpdateVideoStateUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SharedCallingViewModelTest {

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

    @MockK
    private lateinit var callRinger: CallRinger

    private lateinit var sharedCallingViewModel: SharedCallingViewModel


    @BeforeEach
    fun setup() {
        val scheduler = TestCoroutineScheduler()
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        val dummyConversationId = "some-dummy-value@some.dummy.domain"
        MockKAnnotations.init(this)
        every { savedStateHandle.get<String>(any()) } returns dummyConversationId
        every { savedStateHandle.set(any(), any<String>()) } returns Unit

        sharedCallingViewModel = SharedCallingViewModel(
            savedStateHandle = savedStateHandle,
            navigationManager = navigationManager,
            conversationDetails = observeConversationDetails,
            allCalls = allCalls,
            endCall = endCall,
            muteCall = muteCall,
            unMuteCall = unMuteCall,
            setVideoPreview = setVideoPreview,
            updateVideoState = updateVideoState,
            callRinger = callRinger
        )
    }

    @Test
    fun `given muteOrUnMuteCall is called, when active call is muted, then un-mute the call`() {
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isMuted = false)
        coEvery { muteCall.invoke() } returns Unit

        runTest { sharedCallingViewModel.toggleMute() }

        coVerify(exactly = 1) { muteCall.invoke() }
        sharedCallingViewModel.callState.isMuted shouldBeEqualTo true
    }

    @Test
    fun `given muteOrUnMuteCall is called, when active call is un-muted, then mute the call`() {
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isMuted = true)
        coEvery { unMuteCall.invoke() } returns Unit

        runTest { sharedCallingViewModel.toggleMute() }

        coVerify(exactly = 1) { unMuteCall.invoke() }
        sharedCallingViewModel.callState.isMuted shouldBeEqualTo false

    }

    @Test
    fun `given camera is turned on, when toggling video, then turn off video`() {
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isCameraOn = true)
        coEvery { updateVideoState(any(), any()) } returns Unit

        runTest { sharedCallingViewModel.toggleVideo() }

        coVerify(exactly = 1) { updateVideoState(any(), any()) }
        sharedCallingViewModel.callState.isCameraOn shouldBeEqualTo false
    }

    @Test
    fun `given camera is turned off, when toggling video, then turn on video`() {
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isCameraOn = false)
        coEvery { updateVideoState(any(), any()) } returns Unit

        runTest { sharedCallingViewModel.toggleVideo() }

        coVerify(exactly = 1) { updateVideoState(any(), any()) }
        sharedCallingViewModel.callState.isCameraOn shouldBeEqualTo true
    }

    @Test
    fun `given active call, when user end call, then invoke endCall useCase`() {
        coEvery { navigationManager.navigateBack() } returns Unit
        coEvery { endCall.invoke(any()) } returns Unit
        every { callRinger.stop() } returns Unit

        runTest { sharedCallingViewModel.hangUpCall() }

        coVerify(exactly = 1) { endCall.invoke(any()) }
        coVerify(exactly = 1) { callRinger.stop() }
        coVerify(exactly = 1) { navigationManager.navigateBack() }
    }

}
