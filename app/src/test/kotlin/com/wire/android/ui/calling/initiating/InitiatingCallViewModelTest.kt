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

package com.wire.android.ui.calling.initiating

import androidx.lifecycle.SavedStateHandle
import com.wire.android.media.CallRinger
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.IsLastCallClosedUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.StartCallUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InitiatingCallViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given an outgoing call, when the user ends call, then invoke endCall useCase and close the screen`() = runTest(dispatcher) {
        // Given
        val (arrangement, viewModel) = Arrangement()
            .withEndingCall()
            .withStartCallSucceeding()
            .arrange()

        // When
        viewModel.hangUpCall()
        advanceUntilIdle()

        // Then
        with(arrangement) {
            coVerify(exactly = 1) { endCall(any()) }
            coVerify(exactly = 1) { callRinger.stop() }
            coVerify(exactly = 1) { navigationManager.navigateBack() }
        }
    }

    @Test
    fun `given a start call error, when user tries to start a call, call ring tone is not called`() = runTest {
        // Given
        val (arrangement, viewModel) = Arrangement()
            .withNoInternetConnection()
            .withStartCallSucceeding()
            .arrange()

        // When
        viewModel.initiateCall()

        // Then
        with(arrangement) {
            coVerify(exactly = 0) { callRinger.ring(any()) }
        }
    }

    private class Arrangement {

        @MockK
        private lateinit var savedStateHandle: SavedStateHandle

        @MockK
        private lateinit var establishedCalls: ObserveEstablishedCallsUseCase

        @MockK
        private lateinit var isLastCallClosed: IsLastCallClosedUseCase

        @MockK
        private lateinit var startCall: StartCallUseCase

        @MockK
        private lateinit var qualifiedIdMapper: QualifiedIdMapper

        @MockK
        lateinit var callRinger: CallRinger

        @MockK
        lateinit var endCall: EndCallUseCase

        @MockK
        lateinit var navigationManager: NavigationManager

        val initiatingCallViewModel by lazy {
            InitiatingCallViewModel(
                savedStateHandle = savedStateHandle,
                navigationManager = navigationManager,
                observeEstablishedCalls = establishedCalls,
                startCall = startCall,
                endCall = endCall,
                isLastCallClosed = isLastCallClosed,
                callRinger = callRinger,
                qualifiedIdMapper = qualifiedIdMapper
            )
        }

        init {
            val dummyConversationId = ConversationId("some-dummy-value", "some.dummy.domain")
            MockKAnnotations.init(this)
            every { savedStateHandle.get<String>(any()) } returns "${dummyConversationId.value}@${dummyConversationId.domain}"
            every { savedStateHandle.set(any(), any<String>()) } returns Unit
            every {
                qualifiedIdMapper.fromStringToQualifiedID("some-dummy-value@some.dummy.domain")
            } returns QualifiedID("some-dummy-value", "some.dummy.domain")
            coEvery { isLastCallClosed.invoke(any(), any()) } returns flowOf(false)
            coEvery { navigationManager.navigateBack(any()) } returns Unit
            coEvery { establishedCalls() } returns flowOf(emptyList())
            every { callRinger.ring(any(), any(), any()) } returns Unit
        }

        fun withEndingCall(): Arrangement = apply {
            coEvery { endCall(any()) } returns Unit
            every { callRinger.stop() } returns Unit
            coEvery { navigationManager.navigateBack() } returns Unit
        }

        fun withNoInternetConnection(): Arrangement = apply {
            coEvery { startCall(any(), any()) } returns StartCallUseCase.Result.SyncFailure
            every { callRinger.stop() } returns Unit
            coEvery { navigationManager.navigateBack() } returns Unit
        }

        fun withStartCallSucceeding() = apply {
            coEvery { startCall(any(), any()) } returns StartCallUseCase.Result.Success
        }

        fun arrange() = this to initiatingCallViewModel
    }
}
