/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.media.audiomessage

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.ScopedArgsTestExtension
import com.wire.android.di.scopedArgs
import com.wire.android.media.audiomessage.ConversationAudioMessagePlayer.MessageIdWrapper
import com.wire.kalium.logic.data.id.ConversationId
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(ScopedArgsTestExtension::class)
class AudioMessageViewModelTest {

    @Test
    fun `given audio state is updated with waves mask, when observing, then update the state properly`() = runTest {
        val audioState = AudioState.DEFAULT.copy(audioMediaPlayingState = AudioMediaPlayingState.Fetching)
        val audioMessageFlow = MutableStateFlow<Map<MessageIdWrapper, AudioState>>(emptyMap())
        val (arrangement, viewModel) = Arrangement()
            .withAudioMessageStateFlow(audioMessageFlow)
            .arrange()
        val messageIdWrapper = arrangement.audioMessageArgs.toMessageIdWrapper()

        // state not yet added to the flow
        assertEquals(AudioState.DEFAULT, viewModel.state.audioState)

        // add the state to the flow but waves mask is not yet fetched
        audioMessageFlow.value = mapOf(messageIdWrapper to audioState)
        advanceUntilIdle()
        assertEquals(audioState, viewModel.state.audioState)

        // waves mask fetched
        val wavesMask = listOf(1, 2, 3)
        val audioStateWithWavesMask = audioState.copy(wavesMask = wavesMask)
        audioMessageFlow.value = mapOf(messageIdWrapper to audioStateWithWavesMask)
        advanceUntilIdle()
        assertEquals(audioStateWithWavesMask, viewModel.state.audioState)
    }

    @Test
    fun `given audio speed is updated, when observing, then update the state properly`() = runTest {
        val audioSpeedFlow = MutableStateFlow(AudioSpeed.NORMAL)
        val (arrangement, viewModel) = Arrangement()
            .withAudioSpeedFlow(audioSpeedFlow)
            .arrange()

        // speed not changed yet
        assertEquals(AudioSpeed.NORMAL, viewModel.state.audioSpeed)

        // speed changed
        audioSpeedFlow.value = AudioSpeed.FAST
        advanceUntilIdle()
        assertEquals(AudioSpeed.FAST, viewModel.state.audioSpeed)
    }

    @Test
    fun `given audio message, when initializing, then fetch waves mask only once`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .arrange()
        val (conversationId, messageId) = arrangement.audioMessageArgs
        advanceUntilIdle()

        coVerify(exactly = 1) {
            arrangement.audioMessagePlayer.getOrBuildWavesMask(conversationId, messageId)
        }
    }

    @Test
    fun `given audio message, when play audio executed, then call proper action`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .arrange()
        val (conversationId, messageId) = arrangement.audioMessageArgs
        advanceUntilIdle()

        viewModel.playAudio()

        coVerify(exactly = 1) {
            arrangement.audioMessagePlayer.playAudio(conversationId, messageId)
        }
    }

    @Test
    fun `given audio message, when audio position changed, then call proper action`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .arrange()
        val (conversationId, messageId) = arrangement.audioMessageArgs
        val position = 10f
        advanceUntilIdle()

        viewModel.changeAudioPosition(position)

        coVerify(exactly = 1) {
            arrangement.audioMessagePlayer.setPosition(conversationId, messageId, position.toInt())
        }
    }

    @Test
    fun `given audio message, when audio speed changed, then call proper action`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .arrange()
        val audioSpeed = AudioSpeed.FAST
        advanceUntilIdle()

        viewModel.changeAudioSpeed(audioSpeed)

        coVerify(exactly = 1) {
            arrangement.audioMessagePlayer.setSpeed(audioSpeed)
        }
    }

    inner class Arrangement {

        @MockK
        lateinit var audioMessagePlayer: ConversationAudioMessagePlayer

        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        val audioMessageArgs = AudioMessageArgs(ConversationId("convId", "domain"), "msgId")

        init {
            MockKAnnotations.init(this, relaxed = true)
            every { savedStateHandle.scopedArgs<AudioMessageArgs>() } returns audioMessageArgs
        }

        fun withAudioMessageStateFlow(flow: Flow<Map<MessageIdWrapper, AudioState>>) = apply {
            every { audioMessagePlayer.observableAudioMessagesState } returns flow
        }

        fun withAudioSpeedFlow(flow: Flow<AudioSpeed>) = apply {
            every { audioMessagePlayer.audioSpeed } returns flow
        }

        fun arrange() = this to AudioMessageViewModelImpl(audioMessagePlayer, savedStateHandle)
    }
}

private fun AudioMessageArgs.toMessageIdWrapper() = MessageIdWrapper(conversationId, messageId)
