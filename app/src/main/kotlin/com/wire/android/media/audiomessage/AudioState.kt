/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

import androidx.annotation.StringRes
import com.wire.android.R
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.media.player.AudioMediaPlayingState
import com.wire.media.player.AudioState

typealias AudioSpeed = com.wire.media.player.PlaybackSpeed

sealed class PlayingAudioMessage {
    data object None : PlayingAudioMessage()
    data class Some(
        val conversationId: ConversationId,
        val messageId: String,
        val authorName: UIText,
        val state: AudioState
    ) : PlayingAudioMessage()

    fun isSameAs(that: PlayingAudioMessage): Boolean {
        val isTypeSame = (this is Some && that is Some)
                || (this is None && that is None)

        val isMessageSame = this is Some && that is Some
                && this.messageId == that.messageId
                && this.state.isPlaying() == that.state.isPlaying()

        return isTypeSame && isMessageSame
    }
}

@get:StringRes
val AudioSpeed.titleRes: Int
    get() = when (this) {
        AudioSpeed.NORMAL -> R.string.audio_speed_1
        AudioSpeed.FAST -> R.string.audio_speed_1_5
        AudioSpeed.MAX -> R.string.audio_speed_2
    }

sealed class AudioMediaPlayerStateUpdate(
    open val conversationId: ConversationId,
    open val messageId: String
) {
    data class AudioMediaPlayingStateUpdate(
        override val conversationId: ConversationId,
        override val messageId: String,
        val audioMediaPlayingState: AudioMediaPlayingState
    ) : AudioMediaPlayerStateUpdate(conversationId, messageId)

    data class PositionChangeUpdate(
        override val conversationId: ConversationId,
        override val messageId: String,
        val position: Int
    ) : AudioMediaPlayerStateUpdate(conversationId, messageId)

    data class TotalTimeUpdate(
        override val conversationId: ConversationId,
        override val messageId: String,
        val totalTimeInMs: Int
    ) : AudioMediaPlayerStateUpdate(conversationId, messageId)
}

sealed class RecordAudioMediaPlayerStateUpdate {
    data class RecordAudioMediaPlayingStateUpdate(
        val audioMediaPlayingState: AudioMediaPlayingState
    ) : RecordAudioMediaPlayerStateUpdate()

    data class PositionChangeUpdate(
        val position: Int
    ) : RecordAudioMediaPlayerStateUpdate()

    data class TotalTimeUpdate(
        val totalTimeInMs: Int
    ) : RecordAudioMediaPlayerStateUpdate()
}
