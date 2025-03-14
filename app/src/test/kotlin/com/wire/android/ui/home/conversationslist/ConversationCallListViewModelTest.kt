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
@file:Suppress("MaxLineLength", "MaximumLineLength")

package com.wire.android.ui.home.conversationslist

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.feature.call.usecase.AnswerCallUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class ConversationCallListViewModelTest {

    private val dispatcherProvider = TestDispatcherProvider()

    @Test
    fun `given a conversation id, when joining an ongoing call, then verify that answer call usecase is called`() =
        runTest(dispatcherProvider.main()) {
        // Given
        val (arrangement, conversationCallListViewModel) = Arrangement().arrange()

        // When
        conversationCallListViewModel.joinOngoingCall(conversationId = conversationId, onJoined = arrangement.onJoined)

        // Then
        coVerify(exactly = 1) { arrangement.answerCall(conversationId = conversationId) }
        verify(exactly = 1) { arrangement.onJoined(conversationId) }
    }

    @Test
    fun `given join dialog displayed, when user dismiss it, then hide it`() = runTest(dispatcherProvider.main()) {
        // Given
        val (_, conversationCallListViewModel) = Arrangement()
            .withEstablishedCall(call)
            .arrange()
        conversationCallListViewModel.joinCallDialogState.show(call.conversationId)

        // When
        conversationCallListViewModel.joinCallDialogState.dismiss()

        // Then
        assertEquals(false, conversationCallListViewModel.joinCallDialogState.isVisible)
    }

    @Test
    fun `given no ongoing call, when user tries to join a call, then invoke answerCall call use case`() =
        runTest(dispatcherProvider.main()) {
        // Given
        val (arrangement, conversationCallListViewModel) = Arrangement()
            .withEstablishedCall(null)
            .arrange()

        // When
        conversationCallListViewModel.joinOngoingCall(conversationId, arrangement.onJoined)

        // Then
        coVerify(exactly = 1) { arrangement.answerCall(conversationId = any()) }
        coVerify(exactly = 1) { arrangement.onJoined(any()) }
        assertEquals(false, conversationCallListViewModel.joinCallDialogState.isVisible)
    }

    @Test
    fun `given an ongoing call, when user tries to join a call, then show JoinCallAnywayDialog`() = runTest(dispatcherProvider.main()) {
        // Given
        val (arrangement, conversationCallListViewModel) = Arrangement()
            .withEstablishedCall(call)
            .arrange()

        // When
        conversationCallListViewModel.joinOngoingCall(conversationId, arrangement.onJoined)

        // Then
        assertEquals(true, conversationCallListViewModel.joinCallDialogState.isVisible)
        coVerify(inverse = true) { arrangement.answerCall(conversationId = any()) }
    }

    @Test
    fun `given an ongoing call, when user confirms dialog to join a call, then end current call and join the newer one`() =
        runTest(dispatcherProvider.main()) {
        // Given
        val (arrangement, conversationCallListViewModel) = Arrangement()
            .withEstablishedCall(call)
            .arrange()
        conversationCallListViewModel.joinCallDialogState.show(call.conversationId)

        // When
        conversationCallListViewModel.joinAnyway(conversationId, arrangement.onJoined)

        // Then
        coVerify(exactly = 1) { arrangement.endCall(any()) }
    }

    inner class Arrangement {

        @MockK
        lateinit var answerCall: AnswerCallUseCase

        @MockK
        lateinit var endCall: EndCallUseCase

        @MockK
        lateinit var observeEstablishedCalls: ObserveEstablishedCallsUseCase

        @MockK(relaxed = true)
        lateinit var onJoined: (ConversationId) -> Unit

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { observeEstablishedCalls.invoke() } returns emptyFlow()
            mockUri()
        }

        fun withEstablishedCall(call: Call? = null) = apply {
            coEvery { observeEstablishedCalls.invoke() } returns flowOf(listOfNotNull(call))
        }

        fun arrange() = this to ConversationCallListViewModelImpl(
            answerCall = answerCall,
            endCall = endCall,
            observeEstablishedCalls = observeEstablishedCalls
        )
    }

    companion object {
        private val conversationId = ConversationId("some_id", "some_domain")
        private val call = Call(
            conversationId = ConversationId("ongoing_call_id", "some_domain"),
            status = CallStatus.ESTABLISHED,
            isMuted = false,
            isCameraOn = true,
            isCbrEnabled = false,
            callerId = QualifiedID("some_id", "some_domain"),
            conversationName = "some_name",
            conversationType = Conversation.Type.GROUP,
            callerName = "some_name",
            callerTeamName = "some_team_name"
        )
    }
}
