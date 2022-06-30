package com.wire.android.ui.calling

import android.view.View
import androidx.lifecycle.SavedStateHandle
import com.wire.android.mapper.UICallParticipantMapper
import com.wire.android.media.CallRinger
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.call.VideoState
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.GetAllCallsWithSortedParticipantsUseCase
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
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.call.usecase.ObserveSpeakerUseCase
import com.wire.kalium.logic.feature.call.usecase.TurnLoudSpeakerOffUseCase
import com.wire.kalium.logic.feature.call.usecase.TurnLoudSpeakerOnUseCase
import io.mockk.mockk
import io.mockk.verify

class SharedCallingViewModelTest {

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    private lateinit var navigationManager: NavigationManager

    @MockK
    private lateinit var allCalls: GetAllCallsWithSortedParticipantsUseCase

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
    private lateinit var turnLoudSpeakerOff: TurnLoudSpeakerOffUseCase

    @MockK
    private lateinit var turnLoudSpeakerOn: TurnLoudSpeakerOnUseCase

    @MockK
    private lateinit var observeSpeaker: ObserveSpeakerUseCase

    @MockK
    private lateinit var callRinger: CallRinger

    @MockK
    private lateinit var view: View

    @MockK
    private lateinit var wireSessionImageLoader: WireSessionImageLoader

    private val uiCallParticipantMapper: UICallParticipantMapper = UICallParticipantMapper(wireSessionImageLoader)

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
            turnLoudSpeakerOff = turnLoudSpeakerOff,
            turnLoudSpeakerOn = turnLoudSpeakerOn,
            observeSpeaker = observeSpeaker,
            callRinger = callRinger,
            uiCallParticipantMapper = uiCallParticipantMapper,
            wireSessionImageLoader = wireSessionImageLoader
        )
    }

    @Test
    fun `given muteOrUnMuteCall is called, when active call is muted, then un-mute the call`() {
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isMuted = false)
        coEvery { muteCall(conversationId) } returns Unit

        runTest { sharedCallingViewModel.toggleMute() }

        sharedCallingViewModel.callState.isMuted shouldBeEqualTo true
    }

    @Test
    fun `given muteOrUnMuteCall is called, when active call is un-muted, then mute the call`() {
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isMuted = true)
        coEvery { unMuteCall(conversationId) } returns Unit

        runTest { sharedCallingViewModel.toggleMute() }

        sharedCallingViewModel.callState.isMuted shouldBeEqualTo false
    }

    @Test
    fun `given camera is turned on, when toggling video, then turn off video and speaker`() {
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isCameraOn = true)
        coEvery { updateVideoState(any(), any()) } returns Unit
        every { turnLoudSpeakerOff() } returns Unit

        runTest { sharedCallingViewModel.toggleVideo() }

        verify(exactly = 1) { turnLoudSpeakerOff() }
        sharedCallingViewModel.callState.isCameraOn shouldBeEqualTo false
        sharedCallingViewModel.callState.isSpeakerOn shouldBeEqualTo false
    }

    @Test
    fun `given camera is turned off, when toggling video, then turn on video and speaker`() {
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isCameraOn = false)
        coEvery { updateVideoState(any(), any()) } returns Unit
        every { turnLoudSpeakerOn() } returns Unit

        runTest { sharedCallingViewModel.toggleVideo() }

        verify(exactly = 1) { turnLoudSpeakerOn() }
        sharedCallingViewModel.callState.isCameraOn shouldBeEqualTo true
        sharedCallingViewModel.callState.isSpeakerOn shouldBeEqualTo true
    }

    @Test
    fun `given an active call, when the user ends call, then invoke endCall useCase`() {
        coEvery { endCall(any()) } returns Unit
        every { callRinger.stop() } returns Unit

        runTest { sharedCallingViewModel.hangUpCall() }

        coVerify(exactly = 1) { endCall(any()) }
        coVerify(exactly = 1) { callRinger.stop() }
    }

    @Test
    fun `given an active call, when setVideoPreview is called, then set the video preview and update video state to STARTED`() {
        coEvery { setVideoPreview(any(), any()) } returns Unit
        coEvery { updateVideoState(any(), any()) } returns Unit

        runTest {sharedCallingViewModel.setVideoPreview(view) }

        coVerify(exactly = 2) { setVideoPreview(any(), any()) }
        coVerify(exactly = 1) { updateVideoState(any(), VideoState.STARTED) }
    }

    @Test
    fun `given an active call, when clearVideoPreview is called, then clear the video preview and update video state to STOPPED`() {
        coEvery { setVideoPreview(any(), any()) } returns Unit
        coEvery { updateVideoState(any(), any()) } returns Unit

        runTest {sharedCallingViewModel.clearVideoPreview() }

        coVerify(exactly = 1) { setVideoPreview(any(), any()) }
        coVerify(exactly = 1) { updateVideoState(any(), VideoState.STOPPED) }
    }

    @Test
    fun `given an video call, when pauseVideo is called, then clear the video preview and update video state to PAUSED`() {
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isCameraOn = true)
        coEvery { setVideoPreview(any(), any()) } returns Unit
        coEvery { updateVideoState(any(), any()) } returns Unit

        runTest {sharedCallingViewModel.pauseVideo() }

        coVerify(exactly = 1) { setVideoPreview(any(), any()) }
        coVerify(exactly = 1) { updateVideoState(any(), VideoState.PAUSED) }
        sharedCallingViewModel.callState.isCameraOn shouldBeEqualTo false
    }

    @Test
    fun `given an audio call, when pauseVideo is called, then do not pause the video`() {
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isCameraOn = false)
        coEvery { setVideoPreview(any(), any()) } returns Unit
        coEvery { updateVideoState(any(), any()) } returns Unit

        runTest {sharedCallingViewModel.pauseVideo() }

        coVerify(exactly = 0) { setVideoPreview(any(), any()) }
        coVerify(exactly = 0) { updateVideoState(any(), VideoState.PAUSED) }
        sharedCallingViewModel.callState.isCameraOn shouldBeEqualTo false
    }

    companion object {
        private val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")
    }

}
