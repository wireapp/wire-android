package com.wire.android.ui.calling.incoming

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.media.CallRinger
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.feature.call.AnswerCallUseCase
import com.wire.kalium.logic.feature.call.usecase.GetAllCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.RejectCallUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
    lateinit var conversationDetails: ObserveConversationDetailsUseCase

    @MockK
    private lateinit var allCalls: GetAllCallsUseCase

    @MockK
    lateinit var rejectCall: RejectCallUseCase

    @MockK
    lateinit var acceptCall: AnswerCallUseCase

    @MockK
    private lateinit var callRinger: CallRinger

    private lateinit var viewModel: IncomingCallViewModel

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        val dummyConversationId = "some-dummy-value@some.dummy.domain"
        every { savedStateHandle.get<String>(any()) } returns dummyConversationId

        // Default empty values
        coEvery { navigationManager.navigateBack() } returns Unit
        coEvery { navigationManager.navigate(any()) } returns Unit
        coEvery { conversationDetails(any()) } returns flowOf()
        coEvery { rejectCall(any()) } returns Unit
        coEvery { acceptCall(any()) } returns Unit

        viewModel = IncomingCallViewModel(
            savedStateHandle = savedStateHandle,
            navigationManager = navigationManager,
            conversationDetails = conversationDetails,
            allCalls = allCalls,
            rejectCall = rejectCall,
            acceptCall = acceptCall,
            callRinger = callRinger
        )
    }

    @Test
    fun `given an incoming call, when the user decline the call, then the reject call use case is called`() {
        every { callRinger.stop() } returns Unit

        viewModel.declineCall()

        coVerify(exactly = 1) { rejectCall(conversationId = any()) }
        verify(exactly = 1) { callRinger.stop() }
    }

    @Test
    fun `given an incoming call, when the user accepts the call, then the accept call use case is called`() {
        every { callRinger.stop() } returns Unit

        viewModel.acceptCall()

        coVerify(exactly = 1) { acceptCall(conversationId = any()) }
        coVerify(exactly = 1) { navigationManager.navigate(command = any()) }
        verify(exactly = 1) { callRinger.stop() }
    }
}
