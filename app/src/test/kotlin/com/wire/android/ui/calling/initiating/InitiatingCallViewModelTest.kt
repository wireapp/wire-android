package com.wire.android.ui.calling.initiating

import androidx.lifecycle.SavedStateHandle
import com.wire.android.media.CallRinger
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.GetAllCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.StartCallUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InitiatingCallViewModelTest {

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    private lateinit var navigationManager: NavigationManager

    @MockK
    private lateinit var allCalls: GetAllCallsUseCase

    @MockK
    private lateinit var startCall: StartCallUseCase

    @MockK
    private lateinit var endCall: EndCallUseCase

    @MockK
    private lateinit var callRinger: CallRinger

    @MockK
    private lateinit var observeConversationDetails: ObserveConversationDetailsUseCase

    private lateinit var initiatingCallViewModel: InitiatingCallViewModel

    @BeforeEach
    fun setup() {
        val scheduler = TestCoroutineScheduler()
        val dummyConversationId = "some-dummy-value@some.dummy.domain"
        Dispatchers.setMain(StandardTestDispatcher(scheduler))

        MockKAnnotations.init(this)
        every { savedStateHandle.get<String>(any()) } returns dummyConversationId
        every { savedStateHandle.set(any(), any<String>()) } returns Unit

        initiatingCallViewModel = InitiatingCallViewModel(
            savedStateHandle = savedStateHandle,
            navigationManager = navigationManager,
            allCalls = allCalls,
            startCall = startCall,
            endCall = endCall,
            conversationDetails = observeConversationDetails,
            callRinger = callRinger
        )
    }

    @Test
    fun `given active call, when user end call, then invoke endCall useCase`() {
        coEvery { navigationManager.navigateBack() } returns Unit
        coEvery { endCall.invoke(any()) } returns Unit
        every { callRinger.stop() } returns Unit

        runTest { initiatingCallViewModel.hangUpCall() }

        coVerify(exactly = 1) { endCall.invoke(any()) }
        coVerify(exactly = 1) { callRinger.stop() }
        coVerify(exactly = 1) { navigationManager.navigateBack() }
    }

}
