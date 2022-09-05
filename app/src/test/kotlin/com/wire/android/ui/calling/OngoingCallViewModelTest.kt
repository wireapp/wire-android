package com.wire.android.ui.calling

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.navigation.NavigationManager
import com.wire.android.services.ServicesManager
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.call.Call
import com.wire.kalium.logic.feature.call.CallStatus
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class OngoingCallViewModelTest {

    @Test
    fun givenEstablishedCall_whenAppGoesToBackground_thenOngoingCallServiceRun() = runTest {
        val establishedCalls = MutableStateFlow<List<Call>>(listOf(CALL))
        val currentScreenFlow = MutableStateFlow<CurrentScreen>(CurrentScreen.OngoingCallScreen(CONVERSATION_ID))
        val (arrangement, _) = Arrangement()
            .withEstablishedCall(establishedCalls)
            .withCurrentScreen(currentScreenFlow)
            .arrange()

        currentScreenFlow.emit(CurrentScreen.InBackground)

        verify(exactly = 1) { arrangement.servicesManager.startOngoingCallService(any(), CONVERSATION_ID) }
    }

    @Test
    fun givenEstablishedCall_whenAppGoesToOngoingCallScreen_thenOngoingCallServiceStopped() = runTest {
        val establishedCalls = MutableStateFlow<List<Call>>(listOf(CALL))
        val currentScreenFlow = MutableStateFlow<CurrentScreen>(CurrentScreen.OngoingCallScreen(CONVERSATION_ID))
        val (arrangement, _) = Arrangement()
            .withEstablishedCall(establishedCalls)
            .withCurrentScreen(currentScreenFlow)
            .arrange()

        currentScreenFlow.emit(CurrentScreen.OngoingCallScreen(CONVERSATION_ID))

        verify(exactly = 1) { arrangement.servicesManager.stopOngoingCallService() }
    }

    @Test
    fun givenEstablishedCall_whenAppCallEnded_thenOngoingCallServiceStopped() = runTest {
        val establishedCalls = MutableStateFlow<List<Call>>(listOf(CALL))
        val currentScreenFlow = MutableStateFlow<CurrentScreen>(CurrentScreen.OngoingCallScreen(CONVERSATION_ID))
        val (arrangement, _) = Arrangement()
            .withEstablishedCall(establishedCalls)
            .withCurrentScreen(currentScreenFlow)
            .arrange()

        establishedCalls.emit(listOf())

        verify(exactly = 1) { arrangement.servicesManager.stopOngoingCallService() }
    }

    class Arrangement {
        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        @MockK
        lateinit var navigationManager: NavigationManager

        @MockK
        lateinit var establishedCall: ObserveEstablishedCallsUseCase

        @MockK
        lateinit var qualifiedIdMapper: QualifiedIdMapper

        @MockK
        lateinit var servicesManager: ServicesManager

        @MockK
        lateinit var currentScreenManager: CurrentScreenManager

        private val viewModel by lazy {
            OngoingCallViewModel(
                savedStateHandle = savedStateHandle,
                qualifiedIdMapper = qualifiedIdMapper,
                navigationManager = navigationManager,
                establishedCall = establishedCall,
                currentScreenManager = currentScreenManager,
                servicesManager = servicesManager
            )
        }

        init {
            val dummyConversationId = "conversation_value@conversation_domain"
            MockKAnnotations.init(this)
            every { savedStateHandle.get<String>(any()) } returns dummyConversationId
            every { savedStateHandle.set(any(), any<String>()) } returns Unit
            every { servicesManager.startOngoingCallService(any(), any()) } returns Unit
            every { servicesManager.stopOngoingCallService() } returns Unit
            every { qualifiedIdMapper.fromStringToQualifiedID(any()) } returns CONVERSATION_ID
        }

        fun withCurrentScreen(currentScreenFlow: StateFlow<CurrentScreen>) = apply {
            coEvery { currentScreenManager.observeCurrentScreen(any()) } returns currentScreenFlow
        }

        fun withEstablishedCall(callsFlow: Flow<List<Call>>) = apply {
            coEvery { establishedCall.invoke() } returns callsFlow
        }

        fun arrange() = this to viewModel
    }

    companion object {
        val CONVERSATION_ID = ConversationId("conversation_value", "conversation_domain")
        val CALL = Call(
            conversationId = CONVERSATION_ID,
            status = CallStatus.ESTABLISHED,
            callerId = "caller_id",
            participants = listOf(),
            isMuted = false,
            isCameraOn = false,
            maxParticipants = 0,
            conversationName = "ONE_ON_ONE Name",
            conversationType = Conversation.Type.ONE_ON_ONE,
            callerName = "otherUsername",
            callerTeamName = "team_1"
        )
    }
}
