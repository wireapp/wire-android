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

package com.wire.android.ui.calling.incoming

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.media.CallRinger
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.call.usecase.AnswerCallUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.GetIncomingCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.MuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.RejectCallUseCase
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class IncomingCallViewModelTest {

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    lateinit var navigationManager: NavigationManager

    @MockK
    lateinit var rejectCall: RejectCallUseCase

    @MockK
    lateinit var incomingCalls: GetIncomingCallsUseCase

    @MockK
    lateinit var acceptCall: AnswerCallUseCase

    @MockK
    private lateinit var callRinger: CallRinger

    @MockK
    private lateinit var observeEstablishedCalls: ObserveEstablishedCallsUseCase

    @MockK
    private lateinit var endCall: EndCallUseCase

    @MockK
    private lateinit var muteCall: MuteCallUseCase

    @MockK
    private lateinit var qualifiedIdMapper: QualifiedIdMapper

    private lateinit var viewModel: IncomingCallViewModel

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        val dummyConversationId = "some-dummy-value@some.dummy.domain"
        every { savedStateHandle.get<String>(any()) } returns dummyConversationId
        coEvery {
            qualifiedIdMapper.fromStringToQualifiedID("some-dummy-value@some.dummy.domain")
        } returns QualifiedID("some-dummy-value", "some.dummy.domain")

        // Default empty values
        coEvery { navigationManager.navigateBack() } returns Unit
        coEvery { navigationManager.navigate(any()) } returns Unit
        coEvery { rejectCall(any()) } returns Unit
        coEvery { acceptCall(any()) } returns Unit
        every { callRinger.ring(any(), any()) } returns Unit
        every { callRinger.stop() } returns Unit
        coEvery { incomingCalls.invoke() } returns flowOf(emptyList())
        coEvery { observeEstablishedCalls.invoke() } returns flowOf(emptyList())

        viewModel = IncomingCallViewModel(
            savedStateHandle = savedStateHandle,
            navigationManager = navigationManager,
            incomingCalls = incomingCalls,
            rejectCall = rejectCall,
            acceptCall = acceptCall,
            callRinger = callRinger,
            observeEstablishedCalls = observeEstablishedCalls,
            endCall = endCall,
            muteCall = muteCall,
            qualifiedIdMapper = qualifiedIdMapper
        )
    }

    @Test
    fun `given an incoming call, when the user decline the call, then the reject call use case is called`() {
        clearAllMocks() // Ingores calls made during ViewModel initialization
        every { callRinger.stop() } returns Unit
        coEvery { rejectCall(conversationId = any()) } returns Unit
        coEvery { navigationManager.navigateBack() } returns Unit

        viewModel.declineCall()

        coVerify(exactly = 1) { rejectCall(conversationId = any()) }
        verify(exactly = 1) { callRinger.stop() }
    }
    @Test
    fun `given no ongoing call, when user tries to accept an incoming call, then invoke answerCall call use case`() = runTest {
        clearAllMocks() // Ingores calls made during ViewModel initialization
        viewModel.incomingCallState = viewModel.incomingCallState.copy(hasEstablishedCall = false)

        coEvery { navigationManager.navigate(command = any()) } returns Unit
        coEvery { acceptCall(conversationId = any()) } returns Unit
        every { callRinger.stop() } returns Unit

        viewModel.acceptCall()
        advanceUntilIdle()

        coVerify(exactly = 1) { acceptCall(conversationId = any()) }
        coVerify(exactly = 1) { navigationManager.navigate(command = any()) }
        verify(exactly = 1) { callRinger.stop() }
        coVerify(inverse = true) { endCall(any()) }
        assertEquals(false, viewModel.incomingCallState.shouldShowJoinCallAnywayDialog)
    }

    @Test
    fun `given an ongoing call, when user tries to accept an incoming call, then show JoinCallAnywayDialog`() {
        viewModel.incomingCallState = viewModel.incomingCallState.copy(hasEstablishedCall = true)
        clearAllMocks() // Ingores calls made during ViewModel initialization

        viewModel.acceptCall()

        assertEquals(true, viewModel.incomingCallState.shouldShowJoinCallAnywayDialog)
        coVerify(inverse = true) { acceptCall(conversationId = any()) }
        verify(inverse = true) { callRinger.stop() }
    }

    @Test
    fun `given an ongoing call, when user confirms dialog to accept an incoming call, then end current call and accept the newer one`() {
        viewModel.incomingCallState = viewModel.incomingCallState.copy(hasEstablishedCall = true)
        viewModel.establishedCallConversationId = ConversationId("value", "Domain")
        coEvery { muteCall(any(), eq(false)) } returns Unit
        coEvery { endCall(any()) } returns Unit

        viewModel.acceptCallAnyway()

        coVerify(exactly = 1) { endCall(any()) }
        coVerify(exactly = 1) { muteCall(any(), eq(false)) }
    }

    @Test
    fun `given join dialog displayed, when user dismisses it, then hide it`() {
        viewModel.incomingCallState = viewModel.incomingCallState.copy(shouldShowJoinCallAnywayDialog = true)

        viewModel.dismissJoinCallAnywayDialog()

        assertEquals(false, viewModel.incomingCallState.shouldShowJoinCallAnywayDialog)
    }
}
