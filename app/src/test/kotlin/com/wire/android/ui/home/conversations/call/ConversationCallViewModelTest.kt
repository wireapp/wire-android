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
 */
package com.wire.android.ui.home.conversations.call

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.navArgs
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.call.usecase.AnswerCallUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.IsEligibleToStartCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveOngoingCallsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(NavigationTestExtension::class)
class ConversationCallViewModelTest {

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    private lateinit var observeOngoingCalls: ObserveOngoingCallsUseCase

    @MockK
    private lateinit var observeEstablishedCalls: ObserveEstablishedCallsUseCase

    @MockK
    private lateinit var joinCall: AnswerCallUseCase

    @MockK
    private lateinit var endCall: EndCallUseCase

    @MockK
    private lateinit var observeSyncState: ObserveSyncStateUseCase

    @MockK
    private lateinit var isConferenceCallingEnabled: IsEligibleToStartCallUseCase

    @MockK(relaxed = true)
    private lateinit var onAnswered: (conversationId: ConversationId) -> Unit

    @MockK
    private lateinit var observeConversationDetails: ObserveConversationDetailsUseCase

    private lateinit var conversationCallViewModel: ConversationCallViewModel

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")
        every { savedStateHandle.navArgs<ConversationNavArgs>() } returns ConversationNavArgs(conversationId = conversationId)
        coEvery { observeEstablishedCalls.invoke() } returns emptyFlow()
        coEvery { observeOngoingCalls.invoke() } returns emptyFlow()
        coEvery { observeConversationDetails(any()) } returns flowOf()

        conversationCallViewModel = ConversationCallViewModel(
            savedStateHandle = savedStateHandle,
            observeOngoingCalls = observeOngoingCalls,
            observeEstablishedCalls = observeEstablishedCalls,
            answerCall = joinCall,
            endCall = endCall,
            observeSyncState = observeSyncState,
            isConferenceCallingEnabled = isConferenceCallingEnabled,
            observeConversationDetails = observeConversationDetails
        )
    }

    @Test
    fun `given join dialog displayed, when user dismiss it, then hide it`() {
        conversationCallViewModel.conversationCallViewState = conversationCallViewModel.conversationCallViewState.copy(
            shouldShowJoinAnywayDialog = true
        )

        conversationCallViewModel.dismissJoinCallAnywayDialog()

        assertEquals(false, conversationCallViewModel.conversationCallViewState.shouldShowJoinAnywayDialog)
    }

    @Test
    fun `given no ongoing call, when user tries to join a call, then invoke answerCall call use case`() {
        conversationCallViewModel.conversationCallViewState =
            conversationCallViewModel.conversationCallViewState.copy(hasEstablishedCall = false)

        coEvery { joinCall(conversationId = any()) } returns Unit

        conversationCallViewModel.joinOngoingCall(onAnswered)

        coVerify(exactly = 1) { joinCall(conversationId = any()) }
        coVerify(exactly = 1) { onAnswered(any()) }
        assertEquals(false, conversationCallViewModel.conversationCallViewState.shouldShowJoinAnywayDialog)
    }

    @Test
    fun `given an ongoing call, when user tries to join a call, then show JoinCallAnywayDialog`() {
        conversationCallViewModel.conversationCallViewState =
            conversationCallViewModel.conversationCallViewState.copy(hasEstablishedCall = true)

        conversationCallViewModel.joinOngoingCall(onAnswered)

        assertEquals(true, conversationCallViewModel.conversationCallViewState.shouldShowJoinAnywayDialog)
        coVerify(inverse = true) { joinCall(conversationId = any()) }
    }

    @Test
    fun `given an ongoing call, when user confirms dialog to join a call, then end current call and join the newer one`() {
        conversationCallViewModel.conversationCallViewState =
            conversationCallViewModel.conversationCallViewState.copy(hasEstablishedCall = true)
        conversationCallViewModel.establishedCallConversationId = ConversationId("value", "Domain")
        coEvery { endCall(any()) } returns Unit

        conversationCallViewModel.joinAnyway(onAnswered)

        coVerify(exactly = 1) { endCall(any()) }
    }

    @Test
    fun `given permission dialog default state is false, when calling showPermissionDialog, then update the state to true`() = runTest {
        conversationCallViewModel.conversationCallViewState =
            conversationCallViewModel.conversationCallViewState.copy(shouldShowCallingPermissionDialog = false)

        conversationCallViewModel.showCallingPermissionDialog()

        assertEquals(true, conversationCallViewModel.conversationCallViewState.shouldShowCallingPermissionDialog)
    }

    @Test
    fun `given default permission dialog state, when calling dismissPermissionDialog, then update the state to false`() = runTest {
        conversationCallViewModel.conversationCallViewState =
            conversationCallViewModel.conversationCallViewState.copy(shouldShowCallingPermissionDialog = true)

        conversationCallViewModel.dismissCallingPermissionDialog()

        assertEquals(false, conversationCallViewModel.conversationCallViewState.shouldShowCallingPermissionDialog)
    }
}
