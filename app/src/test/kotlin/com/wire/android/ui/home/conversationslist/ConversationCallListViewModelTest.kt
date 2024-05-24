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
import com.wire.android.config.mockUri
import com.wire.android.framework.TestConversationDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.call.usecase.AnswerCallUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationListDetailsUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class ConversationCallListViewModelTest {

    private var conversationCallListViewModel: ConversationCallListViewModel

    @MockK
    lateinit var observeConversationListDetailsUseCase: ObserveConversationListDetailsUseCase

    @MockK
    lateinit var joinCall: AnswerCallUseCase

    @MockK
    private lateinit var endCall: EndCallUseCase

    @MockK
    private lateinit var observeEstablishedCalls: ObserveEstablishedCallsUseCase

    @MockK(relaxed = true)
    private lateinit var onJoined: (ConversationId) -> Unit

    private val dispatcher = StandardTestDispatcher()

    init {
        MockKAnnotations.init(this, relaxUnitFun = true)
        Dispatchers.setMain(dispatcher)

        coEvery { observeEstablishedCalls.invoke() } returns emptyFlow()
        coEvery { observeConversationListDetailsUseCase.invoke(false) } returns flowOf(
            listOf(
                TestConversationDetails.CONNECTION,
                TestConversationDetails.CONVERSATION_ONE_ONE,
                TestConversationDetails.GROUP
            )
        )

        mockUri()
        conversationCallListViewModel =
            ConversationCallListViewModel(
                answerCall = joinCall,
                endCall = endCall,
                observeEstablishedCalls = observeEstablishedCalls
            )
    }

    @Test
    fun `given a conversation id, when joining an ongoing call, then verify that answer call usecase is called`() = runTest {
        coEvery { joinCall(any()) } returns Unit

        conversationCallListViewModel.joinOngoingCall(conversationId = conversationId, onJoined = onJoined)

        coVerify(exactly = 1) { joinCall(conversationId = conversationId) }
        verify(exactly = 1) { onJoined(conversationId) }
    }

    @Test
    fun `given join dialog displayed, when user dismiss it, then hide it`() {
        conversationCallListViewModel.conversationListCallState = conversationCallListViewModel.conversationListCallState.copy(
            shouldShowJoinAnywayDialog = true
        )

        conversationCallListViewModel.dismissJoinCallAnywayDialog()

        assertEquals(false, conversationCallListViewModel.conversationListCallState.shouldShowJoinAnywayDialog)
    }

    @Test
    fun `given no ongoing call, when user tries to join a call, then invoke answerCall call use case`() {
        conversationCallListViewModel.conversationListCallState = conversationCallListViewModel.conversationListCallState.copy(hasEstablishedCall = false)

        coEvery { joinCall(conversationId = any()) } returns Unit

        conversationCallListViewModel.joinOngoingCall(conversationId, onJoined)

        coVerify(exactly = 1) { joinCall(conversationId = any()) }
        coVerify(exactly = 1) { onJoined(any()) }
        assertEquals(false, conversationCallListViewModel.conversationListCallState.shouldShowJoinAnywayDialog)
    }

    @Test
    fun `given an ongoing call, when user tries to join a call, then show JoinCallAnywayDialog`() {
        conversationCallListViewModel.conversationListCallState = conversationCallListViewModel.conversationListCallState.copy(hasEstablishedCall = true)

        conversationCallListViewModel.joinOngoingCall(conversationId, onJoined)

        assertEquals(true, conversationCallListViewModel.conversationListCallState.shouldShowJoinAnywayDialog)
        coVerify(inverse = true) { joinCall(conversationId = any()) }
    }

    @Test
    fun `given an ongoing call, when user confirms dialog to join a call, then end current call and join the newer one`() {
        conversationCallListViewModel.conversationListCallState = conversationCallListViewModel.conversationListCallState.copy(hasEstablishedCall = true)
        conversationCallListViewModel.establishedCallConversationId = ConversationId("value", "Domain")
        coEvery { endCall(any()) } returns Unit

        conversationCallListViewModel.joinAnyway(conversationId, onJoined)

        coVerify(exactly = 1) { endCall(any()) }
    }

    companion object {
        private val conversationId = ConversationId("some_id", "some_domain")
    }
}
