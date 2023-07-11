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

package com.wire.android.ui.calling

import android.view.View
import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.mapper.UICallParticipantMapper
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.media.CallRinger
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.call.VideoState
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.FlipToBackCameraUseCase
import com.wire.kalium.logic.feature.call.usecase.FlipToFrontCameraUseCase
import com.wire.kalium.logic.feature.call.usecase.GetAllCallsWithSortedParticipantsUseCase
import com.wire.kalium.logic.feature.call.usecase.MuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveSpeakerUseCase
import com.wire.kalium.logic.feature.call.usecase.SetVideoPreviewUseCase
import com.wire.kalium.logic.feature.call.usecase.TurnLoudSpeakerOffUseCase
import com.wire.kalium.logic.feature.call.usecase.TurnLoudSpeakerOnUseCase
import com.wire.kalium.logic.feature.call.usecase.UnMuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.UpdateVideoStateUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveSecurityClassificationLabelUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
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
    private lateinit var flipToBackCamera: FlipToBackCameraUseCase

    @MockK
    private lateinit var flipToFrontCamera: FlipToFrontCameraUseCase

    @MockK
    private lateinit var observeSpeaker: ObserveSpeakerUseCase

    @MockK
    private lateinit var qualifiedIdMapper: QualifiedIdMapper

    @MockK
    private lateinit var callRinger: CallRinger

    @MockK
    private lateinit var view: View

    @MockK
    private lateinit var wireSessionImageLoader: WireSessionImageLoader

    @MockK
    private lateinit var userTypeMapper: UserTypeMapper

    @MockK
    private lateinit var currentScreenManager: CurrentScreenManager

    @MockK
    private lateinit var observeSecurityClassificationLabel: ObserveSecurityClassificationLabelUseCase

    private val uiCallParticipantMapper: UICallParticipantMapper by lazy { UICallParticipantMapper(wireSessionImageLoader, userTypeMapper) }

    private lateinit var sharedCallingViewModel: SharedCallingViewModel

    @BeforeEach
    fun setup() {
        val dummyConversationId = "some-dummy-value@some.dummy.domain"
        MockKAnnotations.init(this)
        every { savedStateHandle.get<String>(any()) } returns dummyConversationId
        every { savedStateHandle.set(any(), any<String>()) } returns Unit
        coEvery { allCalls.invoke() } returns emptyFlow()
        coEvery { observeConversationDetails.invoke(any()) } returns emptyFlow()
        coEvery { observeSecurityClassificationLabel.invoke(any()) } returns emptyFlow()
        coEvery { observeSpeaker.invoke() } returns emptyFlow()
        coEvery { currentScreenManager.observeCurrentScreen(any()) } returns MutableStateFlow(CurrentScreen.SomeOther)
        coEvery {
            qualifiedIdMapper.fromStringToQualifiedID("some-dummy-value@some.dummy.domain")
        } returns QualifiedID("some-dummy-value", "some.dummy.domain")

        sharedCallingViewModel = SharedCallingViewModel(
            savedStateHandle = savedStateHandle,
            navigationManager = navigationManager,
            conversationDetails = observeConversationDetails,
            allCalls = allCalls,
            endCall = endCall,
            muteCall = muteCall,
            flipToFrontCamera = flipToFrontCamera,
            flipToBackCamera = flipToBackCamera,
            unMuteCall = unMuteCall,
            setVideoPreview = setVideoPreview,
            updateVideoState = updateVideoState,
            turnLoudSpeakerOff = turnLoudSpeakerOff,
            turnLoudSpeakerOn = turnLoudSpeakerOn,
            observeSpeaker = observeSpeaker,
            callRinger = callRinger,
            uiCallParticipantMapper = uiCallParticipantMapper,
            wireSessionImageLoader = wireSessionImageLoader,
            userTypeMapper = userTypeMapper,
            currentScreenManager = currentScreenManager,
            qualifiedIdMapper = qualifiedIdMapper,
            observeSecurityClassificationLabel = observeSecurityClassificationLabel,
            dispatchers = TestDispatcherProvider()
        )
    }

    @Test
    fun `given isMuted value is null, when toggling microphone, then do not update microphone state`() = runTest {
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isMuted = null)
        coEvery { muteCall(conversationId) } returns Unit

        sharedCallingViewModel.toggleMute()

        sharedCallingViewModel.callState.isMuted shouldBeEqualTo null
    }

    @Test
    fun `given an un-muted call, when toggling microphone, then mute the call`() = runTest {
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isMuted = false)
        coEvery { muteCall(conversationId) } returns Unit

        sharedCallingViewModel.toggleMute()
        advanceUntilIdle()

        coVerify(exactly = 1) { muteCall(any()) }
        sharedCallingViewModel.callState.isMuted shouldBeEqualTo true
    }

    @Test
    fun `given a muted call, when toggling microphone, then un-mute the call`() = runTest {
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isMuted = true)
        coEvery { unMuteCall(any()) } returns Unit

        sharedCallingViewModel.toggleMute()
        advanceUntilIdle()

        coVerify(exactly = 1) { unMuteCall(any()) }
        sharedCallingViewModel.callState.isMuted shouldBeEqualTo false
    }

    @Test
    fun `given user on a preview screen, when muting microphone, then mute the call with false param`() = runTest {
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isMuted = false)
        coEvery { muteCall(conversationId, false) } returns Unit

        sharedCallingViewModel.toggleMute(true)
        advanceUntilIdle()

        coVerify(exactly = 1) { muteCall(any(), false) }
        sharedCallingViewModel.callState.isMuted shouldBeEqualTo true
    }

    @Test
    fun `given user on a preview screen, when un-muting microphone, then un-mute the call with false param`() = runTest {
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isMuted = true)
        coEvery { unMuteCall(conversationId, false) } returns Unit

        sharedCallingViewModel.toggleMute(true)
        advanceUntilIdle()

        coVerify(exactly = 1) { unMuteCall(any(), false) }
        sharedCallingViewModel.callState.isMuted shouldBeEqualTo false
    }

    @Test
    fun `given front facing camera, when flipping it, then switch to back camera`() = runTest {
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isOnFrontCamera = true)
        coEvery { flipToBackCamera(conversationId) } returns Unit

        sharedCallingViewModel.flipCamera()
        advanceUntilIdle()

        coVerify(exactly = 1) { flipToBackCamera(any()) }
        sharedCallingViewModel.callState.isOnFrontCamera shouldBeEqualTo false
    }

    @Test
    fun `given back facing camera, when flipping it, then switch to front camera`() = runTest {
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isOnFrontCamera = false)
        coEvery { flipToFrontCamera(conversationId) } returns Unit

        sharedCallingViewModel.flipCamera()
        advanceUntilIdle()

        coVerify(exactly = 1) { flipToFrontCamera(any()) }
        sharedCallingViewModel.callState.isOnFrontCamera shouldBeEqualTo true
    }

    @Test
    fun `given camera is turned on, when toggling video, then turn off video`() = runTest {
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isCameraOn = true)
        coEvery { updateVideoState(any(), any()) } returns Unit

        sharedCallingViewModel.toggleVideo()
        advanceUntilIdle()

        sharedCallingViewModel.callState.isCameraOn shouldBeEqualTo false
    }

    @Test
    fun `given camera is turned off, when toggling video, then turn on video`() = runTest {
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isCameraOn = false)
        coEvery { updateVideoState(any(), any()) } returns Unit

        sharedCallingViewModel.toggleVideo()
        advanceUntilIdle()

        sharedCallingViewModel.callState.isCameraOn shouldBeEqualTo true
    }

    @Test
    fun `given an active call, when the user ends call, then invoke endCall useCase`() = runTest {
        coEvery { endCall(any()) } returns Unit
        coEvery { muteCall(any(), false) } returns Unit
        every { callRinger.stop() } returns Unit
        coEvery { navigationManager.navigateBack() } returns Unit

        sharedCallingViewModel.hangUpCall()
        advanceUntilIdle()

        coVerify(exactly = 1) { endCall(any()) }
        coVerify(exactly = 1) { muteCall(any(), false) }
        coVerify(exactly = 1) { callRinger.stop() }
    }

    @Test
    fun `given an active call, when setVideoPreview is called, then set the video preview and update video state to STARTED`() = runTest {
        coEvery { setVideoPreview(any(), any()) } returns Unit
        coEvery { updateVideoState(any(), any()) } returns Unit

        sharedCallingViewModel.setVideoPreview(view)
        advanceUntilIdle()

        coVerify(exactly = 2) { setVideoPreview(any(), any()) }
        coVerify(exactly = 1) { updateVideoState(any(), VideoState.STARTED) }
    }

    @Test
    fun `given an active call, when clearVideoPreview is called, then update video state to STOPPED`() = runTest {
        coEvery { setVideoPreview(any(), any()) } returns Unit
        coEvery { updateVideoState(any(), any()) } returns Unit

        sharedCallingViewModel.clearVideoPreview()
        advanceUntilIdle()

        coVerify(exactly = 1) { updateVideoState(any(), VideoState.STOPPED) }
    }

    @Test
    fun `given a video call, when stopping video, then clear Video Preview and turn off speaker`() = runTest {
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isCameraOn = true)
        coEvery { setVideoPreview(any(), any()) } returns Unit
        coEvery { updateVideoState(any(), any()) } returns Unit
        coEvery { turnLoudSpeakerOff() } returns Unit

        sharedCallingViewModel.stopVideo()
        advanceUntilIdle()

        coVerify(exactly = 1) { setVideoPreview(any(), any()) }
        coVerify(exactly = 1) { turnLoudSpeakerOff() }
    }

    @Test
    fun `given an audio call, when stopVideo is invoked, then do not do anything`() = runTest {
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isCameraOn = false)
        coEvery { setVideoPreview(any(), any()) } returns Unit
        coEvery { turnLoudSpeakerOff() } returns Unit

        sharedCallingViewModel.stopVideo()
        advanceUntilIdle()

        coVerify(inverse = true) { setVideoPreview(any(), any()) }
        coVerify(inverse = true) { turnLoudSpeakerOff() }
    }

    companion object {
        private val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")
    }
}
