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
import com.wire.android.framework.TestMessage
import com.wire.android.framework.TestMessage.DUMMY_ASSET_LOCAL_DATA
import com.wire.android.framework.TestMessage.DUMMY_ASSET_REMOTE_DATA
import com.wire.android.media.audiomessage.ConversationAudioMessagePlayer.MessageIdWrapper
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.AssetContent
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.feature.message.ObserveMessageByIdUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
    fun `given waves mask is updated, when observing, then update the state properly`() = runTest {
        val observeMessageFlow = MutableSharedFlow<ObserveMessageByIdUseCase.Result>()
        val (arrangement, viewModel) = Arrangement()
            .withObserveMessageByIdFlow(observeMessageFlow)
            .arrange()

        // first emit the audio message without waves mask
        val messageWithoutWavesMask = mockAudioMessage(arrangement.audioMessageArgs, null)
        observeMessageFlow.emit(ObserveMessageByIdUseCase.Result.Success(messageWithoutWavesMask))
        advanceUntilIdle()
        assertEquals(null, viewModel.state.wavesMask)

        // now emit the audio message with normalized loudness
        val wavesMask = listOf(1, 2, 3)
        val messageWithWavesMask = mockAudioMessage(arrangement.audioMessageArgs, wavesMask.toNormalizedLoudness())
        observeMessageFlow.emit(ObserveMessageByIdUseCase.Result.Success(messageWithWavesMask))
        advanceUntilIdle()
        assertEquals(wavesMask, viewModel.state.wavesMask)
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
        lateinit var observeMessageById: ObserveMessageByIdUseCase

        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        val audioMessageArgs = AudioMessageArgs(ConversationId("convId", "domain"), "msgId")

        init {
            MockKAnnotations.init(this, relaxed = true)
            every { savedStateHandle.scopedArgs<AudioMessageArgs>() } returns audioMessageArgs
            withObserveMessageByIdFlow(flowOf(ObserveMessageByIdUseCase.Result.Success(mockAudioMessage(audioMessageArgs))))
        }

        fun withAudioMessageStateFlow(flow: Flow<Map<MessageIdWrapper, AudioState>>) = apply {
            every { audioMessagePlayer.observableAudioMessagesState } returns flow
        }

        fun withAudioSpeedFlow(flow: Flow<AudioSpeed>) = apply {
            every { audioMessagePlayer.audioSpeed } returns flow
        }

        fun withObserveMessageByIdFlow(resultFlow: Flow<ObserveMessageByIdUseCase.Result>) = apply {
            coEvery {
                observeMessageById(audioMessageArgs.conversationId, audioMessageArgs.messageId)
            } returns resultFlow
        }

        fun arrange() = this to AudioMessageViewModelImpl(audioMessagePlayer, observeMessageById, savedStateHandle)
    }

    companion object {
        fun mockAudioMessage(args: AudioMessageArgs, normalizedLoudness: ByteArray? = null) = TestMessage.ASSET_MESSAGE.copy(
            id = args.messageId,
            conversationId = args.conversationId,
            content = MessageContent.Asset(
                AssetContent(
                    1000L,
                    "name",
                    "audio/wav",
                    AssetContent.AssetMetadata.Audio(10000L, normalizedLoudness),
                    DUMMY_ASSET_REMOTE_DATA,
                    DUMMY_ASSET_LOCAL_DATA,
                )
            )
        )
    }
}
