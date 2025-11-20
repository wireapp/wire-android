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

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.ScopedArgs
import com.wire.android.di.ViewModelScopedPreview
import com.wire.android.di.scopedArgs
import com.wire.kalium.logic.data.id.ConversationId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

@ViewModelScopedPreview
interface AudioMessageViewModel {
    val state: AudioMessageState get() = AudioMessageState()
    fun playAudio() {}
    fun changeAudioPosition(position: Float) {}
    fun changeAudioSpeed(audioSpeed: AudioSpeed) {}
}

@HiltViewModel
class AudioMessageViewModelImpl @Inject constructor(
    private val audioMessagePlayer: ConversationAudioMessagePlayer,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), AudioMessageViewModel {

    private val args: AudioMessageArgs = savedStateHandle.scopedArgs()

    override var state: AudioMessageState by mutableStateOf(AudioMessageState())
        private set

    init {
        observeAudioState()
        observeAudioSpeed()
        initWavesMask()
    }

    private fun observeAudioState() {
        viewModelScope.launch {
            audioMessagePlayer.observableAudioMessagesState
                .mapNotNull {
                    it[ConversationAudioMessagePlayer.MessageIdWrapper(args.conversationId, args.messageId)]
                }
                .distinctUntilChanged()
                .collectLatest {
                    state = state.copy(audioState = it)
                }
        }
    }

    private fun observeAudioSpeed() {
        viewModelScope.launch {
            audioMessagePlayer.audioSpeed
                .distinctUntilChanged()
                .collectLatest {
                    state = state.copy(audioSpeed = it)
                }
        }
    }

    private fun initWavesMask() {
        viewModelScope.launch {
            audioMessagePlayer.getOrBuildWavesMask(args.conversationId, args.messageId)
        }
    }

    override fun playAudio() {
        viewModelScope.launch {
            audioMessagePlayer.playAudio(args.conversationId, args.messageId)
        }
    }

    override fun changeAudioPosition(position: Float) {
        viewModelScope.launch {
            audioMessagePlayer.setPosition(args.conversationId, args.messageId, position.toInt())
        }
    }

    override fun changeAudioSpeed(audioSpeed: AudioSpeed) {
        viewModelScope.launch {
            audioMessagePlayer.setSpeed(audioSpeed)
        }
    }
}

@Serializable
data class AudioMessageArgs(
    val conversationId: ConversationId,
    val messageId: String
) : ScopedArgs {
    override val key = "$ARGS_KEY:$conversationId:$messageId"

    companion object {
        const val ARGS_KEY = "AudioMessageArgsKey"
    }
}

@Stable
data class AudioMessageState(
    val audioSpeed: AudioSpeed = AudioSpeed.NORMAL,
    val audioState: AudioState = AudioState.DEFAULT,
)
