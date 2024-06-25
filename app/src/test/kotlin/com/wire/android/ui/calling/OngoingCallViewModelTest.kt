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

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.config.NavigationTestExtension
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.calling.ongoing.OngoingCallViewModel
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.navArgs
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.call.CallClient
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.call.VideoState
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.RequestVideoStreamsUseCase
import com.wire.kalium.logic.feature.call.usecase.video.SetVideoSendStateUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(NavigationTestExtension::class)
@ExtendWith(CoroutineTestExtension::class)
class OngoingCallViewModelTest {

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    private lateinit var establishedCall: ObserveEstablishedCallsUseCase

    @MockK
    private lateinit var requestVideoStreams: RequestVideoStreamsUseCase

    @MockK
    private lateinit var currentScreenManager: CurrentScreenManager

    @MockK
    private lateinit var setVideoSendState: SetVideoSendStateUseCase

    @MockK
    private lateinit var globalDataStore: GlobalDataStore

    private lateinit var ongoingCallViewModel: OngoingCallViewModel

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        every { savedStateHandle.navArgs<CallingNavArgs>() } returns CallingNavArgs(conversationId = conversationId)
        coEvery { establishedCall.invoke() } returns flowOf(listOf(provideCall()))
        coEvery { currentScreenManager.observeCurrentScreen(any()) } returns MutableStateFlow(CurrentScreen.SomeOther)
        coEvery { globalDataStore.getShouldShowDoubleTapToast(any()) } returns false
        coEvery { setVideoSendState.invoke(any(), any()) } returns Unit

        ongoingCallViewModel = OngoingCallViewModel(
            savedStateHandle = savedStateHandle,
            establishedCalls = establishedCall,
            requestVideoStreams = requestVideoStreams,
            currentScreenManager = currentScreenManager,
            currentUserId = currentUserId,
            setVideoSendState = setVideoSendState,
            globalDataStore = globalDataStore,
        )
    }

    @Test
    fun givenAnOngoingCall_WhenTurningOnCamera_ThenSetVideoSendStateToStarted() = runTest {
        ongoingCallViewModel.startSendingVideoFeed()

        coVerify(exactly = 1) { setVideoSendState.invoke(any(), VideoState.STARTED) }
    }

    @Test
    fun givenAnOngoingCall_WhenTurningOffCamera_ThenSetVideoSendStateToStopped() = runTest {
        ongoingCallViewModel.stopSendingVideoFeed()

        coVerify { setVideoSendState.invoke(any(), VideoState.STOPPED) }
    }

    @Test
    fun givenParticipantsList_WhenRequestingVideoStream_ThenRequestItForOnlyParticipantsWithVideoEnabled() = runTest {
        val expectedClients = listOf(
            CallClient(participant1.id.toString(), participant1.clientId),
            CallClient(participant3.id.toString(), participant3.clientId)
        )
        coEvery { requestVideoStreams(conversationId = conversationId, expectedClients) } returns Unit

        ongoingCallViewModel.requestVideoStreams(participants)

        coVerify(exactly = 1) { requestVideoStreams(conversationId, expectedClients) }
    }

    @Test
    fun givenDoubleTabIndicatorIsDisplayed_whenUserTapsOnIt_thenHideIt() = runTest {
        coEvery { globalDataStore.setShouldShowDoubleTapToastStatus(currentUserId.toString(), false) } returns Unit

        ongoingCallViewModel.hideDoubleTapToast()

        assertEquals(false, ongoingCallViewModel.shouldShowDoubleTapToast)
        coVerify(exactly = 1) { globalDataStore.setShouldShowDoubleTapToastStatus(currentUserId.toString(), false) }
    }

    companion object {
        val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")
        val currentUserId = UserId("userId", "some.dummy.domain")
        private val participant1 = UICallParticipant(
            id = QualifiedID("value1", "domain"),
            clientId = "client-id1",
            name = "name1",
            isMuted = false,
            isSpeaking = false,
            isCameraOn = true,
            isSharingScreen = false,
            membership = Membership.None,
            hasEstablishedAudio = true
        )
        private val participant2 = UICallParticipant(
            id = QualifiedID("value2", "domain"),
            clientId = "client-id2",
            name = "name2",
            isMuted = false,
            isSpeaking = false,
            isCameraOn = false,
            isSharingScreen = false,
            membership = Membership.None,
            hasEstablishedAudio = true
        )
        private val participant3 = UICallParticipant(
            id = QualifiedID("value3", "domain"),
            clientId = "client-id3",
            name = "name3",
            isMuted = false,
            isSpeaking = false,
            isCameraOn = true,
            isSharingScreen = true,
            membership = Membership.None,
            hasEstablishedAudio = true
        )
        val participants = listOf(participant1, participant2, participant3)
    }

    private fun provideCall(id: ConversationId = ConversationId("some-dummy-value", "some.dummy.domain")) = Call(
        conversationId = id,
        status = CallStatus.ESTABLISHED,
        callerId = "caller_id",
        participants = listOf(),
        isMuted = false,
        isCameraOn = false,
        maxParticipants = 0,
        conversationName = "ONE_ON_ONE Name",
        conversationType = Conversation.Type.ONE_ON_ONE,
        callerName = "otherUsername",
        callerTeamName = "team_1",
        isCbrEnabled = false
    )
}
