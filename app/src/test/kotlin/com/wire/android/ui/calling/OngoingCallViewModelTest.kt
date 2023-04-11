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
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.calling.ongoing.OngoingCallViewModel
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.util.CurrentScreenManager
import com.wire.kalium.logic.data.call.CallClient
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.RequestVideoStreamsUseCase
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
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Test
import org.junit.jupiter.api.BeforeEach

@OptIn(ExperimentalCoroutinesApi::class)
class OngoingCallViewModelTest {

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    private lateinit var navigationManager: NavigationManager

    @MockK
    private lateinit var establishedCall: ObserveEstablishedCallsUseCase

    @MockK
    private lateinit var requestVideoStreams: RequestVideoStreamsUseCase

    @MockK
    private lateinit var qualifiedIdMapper: QualifiedIdMapper

    @MockK
    private lateinit var currentScreenManager: CurrentScreenManager

    private lateinit var ongoingCallViewModel: OngoingCallViewModel

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

        ongoingCallViewModel = OngoingCallViewModel(
            savedStateHandle = savedStateHandle,
            qualifiedIdMapper = qualifiedIdMapper,
            navigationManager = navigationManager,
            establishedCalls = establishedCall,
            requestVideoStreams = requestVideoStreams,
            currentScreenManager = currentScreenManager
        )
    }

    @Test
    fun givenParticipantsList_WhenRequestingVideoStream_ThenRequestItForOnlyParticipantsWithVideoEnabled() {
        val expectedClients = listOf(
            CallClient(participant1.id.toString(), participant1.clientId),
            CallClient(participant3.id.toString(), participant3.clientId)
        )
        coEvery { requestVideoStreams(conversationId = conversationId, expectedClients) } returns Unit

        ongoingCallViewModel.requestVideoStreams(participants)

        coVerify(exactly = 1) { requestVideoStreams(conversationId, expectedClients) }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    companion object {
        val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")
        private val participant1 = UICallParticipant(
            id = QualifiedID("value1", "domain"),
            clientId = "client-id1",
            name = "name1",
            isMuted = false,
            isSpeaking = false,
            isCameraOn = true,
            isSharingScreen = false,
            membership = Membership.None
        )
        private val participant2 = UICallParticipant(
            id = QualifiedID("value2", "domain"),
            clientId = "client-id2",
            name = "name2",
            isMuted = false,
            isSpeaking = false,
            isCameraOn = false,
            isSharingScreen = false,
            membership = Membership.None
        )
        private val participant3 = UICallParticipant(
            id = QualifiedID("value3", "domain"),
            clientId = "client-id3",
            name = "name3",
            isMuted = false,
            isSpeaking = false,
            isCameraOn = true,
            isSharingScreen = true,
            membership = Membership.None
        )
        val participants = listOf(participant1, participant2, participant3)
    }
}
