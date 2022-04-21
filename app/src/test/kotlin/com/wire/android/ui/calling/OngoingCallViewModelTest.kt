package com.wire.android.ui.calling

import androidx.lifecycle.SavedStateHandle
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.MuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.UnMuteCallUseCase
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
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OngoingCallViewModelTest {

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    private lateinit var navigationManager: NavigationManager

    @MockK
    private lateinit var endCall: EndCallUseCase

    @MockK
    private lateinit var muteCall: MuteCallUseCase

    @MockK
    private lateinit var unMuteCall: UnMuteCallUseCase

    private lateinit var ongoingCallViewModel: OngoingCallViewModel

    @BeforeEach
    fun setup() {
        val scheduler = TestCoroutineScheduler()
        Dispatchers.setMain(StandardTestDispatcher(scheduler))

        MockKAnnotations.init(this)
        every { savedStateHandle.get<String>(any()) } returns ""
        every { savedStateHandle.set(any(), any<String>()) } returns Unit

        ongoingCallViewModel = OngoingCallViewModel(
            savedStateHandle = savedStateHandle,
            navigationManager = navigationManager,
            endCall = endCall,
            muteCall = muteCall,
            unMuteCall = unMuteCall
        )
    }

    @Test
    fun `given muteOrUnMuteCall is called, when active call is muted, then un-mute the call`() {
        coEvery { muteCall.invoke() } returns Unit

        runTest { ongoingCallViewModel.muteOrUnMuteCall() }

        coVerify(exactly = 1) { muteCall.invoke() }
        ongoingCallViewModel.callEstablishedState.isMuted shouldBeEqualTo true
    }

    @Test
    fun `given muteOrUnMuteCall is called, when active call is un-muted, then mute the call`() {
        ongoingCallViewModel.callEstablishedState = ongoingCallViewModel.callEstablishedState.copy(isMuted = true)
        coEvery { unMuteCall.invoke() } returns Unit

        runTest { ongoingCallViewModel.muteOrUnMuteCall() }

        coVerify(exactly = 1) { unMuteCall.invoke() }
        ongoingCallViewModel.callEstablishedState.isMuted shouldBeEqualTo false

    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}
