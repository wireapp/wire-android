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
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.mapper.UICallParticipantMapper
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.media.CallRinger
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.call.VideoState
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
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
        val scheduler = TestCoroutineScheduler()
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        val dummyConversationId = "some-dummy-value@some.dummy.domain"
        MockKAnnotations.init(this)
        every { savedStateHandle.get<String>(any()) } returns dummyConversationId
        every { savedStateHandle.set(any(), any<String>()) } returns Unit
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
    fun `given isMuted value is null, when toggling microphone, then do not update microphone state`() {
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isMuted = null)
        coEvery { muteCall(conversationId) } returns Unit

        runTest { sharedCallingViewModel.toggleMute() }

        sharedCallingViewModel.callState.isMuted shouldBeEqualTo null
    }

    @Test
    fun `given an un-muted call, when toggling microphone, then mute the call`() {
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isMuted = false)
        coEvery { muteCall(conversationId) } returns Unit

        runTest { sharedCallingViewModel.toggleMute() }

        coVerify(exactly = 1) { muteCall(any()) }
        sharedCallingViewModel.callState.isMuted shouldBeEqualTo true
    }

    @Test
    fun `given a muted call, when toggling microphone, then un-mute the call`() {
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isMuted = true)
        coEvery { unMuteCall(any()) } returns Unit

        runTest { sharedCallingViewModel.toggleMute() }

        coVerify(exactly = 1) { unMuteCall(any()) }
        sharedCallingViewModel.callState.isMuted shouldBeEqualTo false
    }

    @Test
    fun `given camera is turned on, when toggling video, then turn off video`() {
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isCameraOn = true)
        coEvery { updateVideoState(any(), any()) } returns Unit

        runTest { sharedCallingViewModel.toggleVideo() }

        sharedCallingViewModel.callState.isCameraOn shouldBeEqualTo false
    }

    @Test
    fun `given camera is turned off, when toggling video, then turn on video`() {
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isCameraOn = false)
        coEvery { updateVideoState(any(), any()) } returns Unit

        runTest { sharedCallingViewModel.toggleVideo() }

        sharedCallingViewModel.callState.isCameraOn shouldBeEqualTo true
    }

    @Test
    fun `given an active call, when the user ends call, then invoke endCall useCase`() {
        coEvery { endCall(any()) } returns Unit
        coEvery { muteCall(any()) } returns Unit
        every { callRinger.stop() } returns Unit

        runTest { sharedCallingViewModel.hangUpCall() }

        coVerify(exactly = 1) { endCall(any()) }
        coVerify(exactly = 1) { muteCall(any()) }
        coVerify(exactly = 1) { callRinger.stop() }
    }

    @Test
    fun `given an active call, when setVideoPreview is called, then set the video preview and update video state to STARTED`() {
        coEvery { setVideoPreview(any(), any()) } returns Unit
        coEvery { updateVideoState(any(), any()) } returns Unit

        runTest { sharedCallingViewModel.setVideoPreview(view) }

        coVerify(exactly = 2) { setVideoPreview(any(), any()) }
        coVerify(exactly = 1) { updateVideoState(any(), VideoState.STARTED) }
    }

    @Test
    fun `given an active call, when clearVideoPreview is called, then clear the video preview and update video state to STOPPED`() {
        coEvery { setVideoPreview(any(), any()) } returns Unit
        coEvery { updateVideoState(any(), any()) } returns Unit

        runTest { sharedCallingViewModel.clearVideoPreview() }

        coVerify(exactly = 1) { updateVideoState(any(), VideoState.STOPPED) }
    }

    @Test
    fun `given an video call, when pauseVideo is called, then clear the video preview and update video state to PAUSED`() {
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isCameraOn = true)
        coEvery { setVideoPreview(any(), any()) } returns Unit
        coEvery { updateVideoState(any(), any()) } returns Unit

        runTest { sharedCallingViewModel.pauseVideo() }

        coVerify(exactly = 1) { updateVideoState(any(), VideoState.PAUSED) }
    }

    @Test
    fun `given an audio call, when pauseVideo is called, then do not pause the video`() {
        sharedCallingViewModel.callState = sharedCallingViewModel.callState.copy(isCameraOn = false)
        coEvery { setVideoPreview(any(), any()) } returns Unit
        coEvery { updateVideoState(any(), any()) } returns Unit

        runTest { sharedCallingViewModel.pauseVideo() }

        coVerify(inverse = true) { setVideoPreview(any(), any()) }
        coVerify(inverse = true) { updateVideoState(any(), VideoState.PAUSED) }
    }

    companion object {
        private val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")
    }
}
