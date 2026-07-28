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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.ScopedArgs
import com.wire.android.di.ViewModelScopedPreview
import com.wire.kalium.logic.data.id.ConversationId
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import okio.Path.Companion.toPath

@ViewModelScopedPreview
interface CellAudioMessageViewModel {
    val state: AudioMessageState get() = AudioMessageState()
    fun playAudio() {}
    fun changeAudioPosition(position: Float) {}
    fun changeAudioSpeed(audioSpeed: AudioSpeed) {}
}

class CellAudioMessageViewModelImpl(
    private val audioMessagePlayer: ConversationAudioMessagePlayer,
    private val args: CellAudioMessageArgs,
    wavesMask: List<Int>?,
) : ViewModel(), CellAudioMessageViewModel {

    override var state: AudioMessageState by mutableStateOf(AudioMessageState(wavesMask = wavesMask))
        private set

    init {
        observeAudioState()
        observeAudioSpeed()
    }

    private fun observeAudioState() {
        viewModelScope.launch {
            audioMessagePlayer.observableAudioMessagesState
                .mapNotNull {
                    it[ConversationAudioMessagePlayer.MessageIdWrapper(args.conversationId, args.attachmentId)]
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

    override fun playAudio() {
        viewModelScope.launch {
            audioMessagePlayer.playAudioFromPath(
                conversationId = args.conversationId,
                attachmentId = args.attachmentId,
                localPath = args.localPath.toPath()
            )
        }
    }

    override fun changeAudioPosition(position: Float) {
        viewModelScope.launch {
            audioMessagePlayer.setPosition(args.conversationId, args.attachmentId, position.toInt())
        }
    }

    override fun changeAudioSpeed(audioSpeed: AudioSpeed) {
        viewModelScope.launch {
            audioMessagePlayer.setSpeed(audioSpeed)
        }
    }
}

@Serializable
data class CellAudioMessageArgs(
    val conversationId: ConversationId,
    val attachmentId: String,
    val localPath: String,
) : ScopedArgs {
    override val key = "$ARGS_KEY:$conversationId:$attachmentId"

    companion object {
        const val ARGS_KEY = "CellAudioMessageArgsKey"
    }
}

@Stable
data class CellAudioMessageState(
    val audioSpeed: AudioSpeed = AudioSpeed.NORMAL,
    val audioState: AudioState = AudioState.DEFAULT,
    val wavesMask: List<Int>? = null
)
