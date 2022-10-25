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
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.RejectCallUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
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
        coEvery { callRinger.ring(any(), any()) } returns Unit
        coEvery { callRinger.stop() } returns Unit

        viewModel = IncomingCallViewModel(
            savedStateHandle = savedStateHandle,
            navigationManager = navigationManager,
            incomingCalls = incomingCalls,
            rejectCall = rejectCall,
            acceptCall = acceptCall,
            callRinger = callRinger,
            observeEstablishedCalls = observeEstablishedCalls,
            endCall = endCall,
            qualifiedIdMapper = qualifiedIdMapper
        )
    }

    @Test
    fun `given an incoming call, when the user decline the call, then the reject call use case is called`() {
        viewModel.declineCall()

        coVerify(exactly = 1) { rejectCall(conversationId = any()) }
        verify(exactly = 1) { callRinger.stop() }
    }

    @Test
    fun `given an incoming call, when the user accepts the call, then the accept call use case is called`() {
        coEvery { endCall(any()) } returns Unit
        coEvery { navigationManager.navigate(command = any()) } returns Unit

        viewModel.acceptCall()

        coVerify(exactly = 1) { acceptCall(conversationId = any()) }
        verify(exactly = 1) { callRinger.stop() }
        coVerify(inverse = true) { endCall(any()) }
    }

    @Test
    fun `given an active call, when accepting a new incoming call, then end the current call and accept the newer one`() = runTest{
        viewModel.establishedCallConversationId = ConversationId("value", "Domain")
        coEvery { endCall(viewModel.establishedCallConversationId!!) } returns Unit

        viewModel.acceptCall()

        verify(exactly = 1) { callRinger.stop() }
        coVerify(exactly = 1) { endCall(viewModel.establishedCallConversationId!!) }
        coVerify(exactly = 1) { acceptCall(conversationId = any()) }
    }
}
