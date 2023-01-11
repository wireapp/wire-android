package com.wire.android.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import javax.inject.Inject

class ConversationMessageAudioPlayer
@Inject constructor(
    private val context: Context,
    private val getMessageAsset: GetMessageAssetUseCase
) {

    private val audioMessageHistory = mutableMapOf<String, AudioState>()

    private val audioSnapshots = mutableMapOf<String, AudioSnapShot>()

    private var currentlyPlayedMessageId: String? = null

    private val mediaPlayer = MediaPlayer().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
        )
        setOnCompletionListener {
            audioSnapshots[currentlyPlayedMessageId!!] = AudioSnapShot(0)
        }
    }

    suspend fun togglePlay(
        conversationId: ConversationId,
        messageId: String
    ) {
        with(mediaPlayer) {
            val toggleCurrentlyPlayedAudio = messageId == currentlyPlayedMessageId

            if (toggleCurrentlyPlayedAudio) {
                if (isPlaying) {
                    pause()
                } else {
                    start()
                }
            } else {
                val audioSnapShot = audioSnapshots[messageId]
                val audioSnapShotExists = audioSnapShot != null

                if (audioSnapShotExists) {
                    switchAudioMessage(
                        messageId = messageId,
                        conversationId = conversationId,
                        seekTo = audioSnapShot!!.timeStamp
                    )
                } else {
                    switchAudioMessage(
                        messageId = messageId,
                        conversationId = conversationId
                    )
                }
            }
        }
    }

    private suspend fun switchAudioMessage(
        messageId: String,
        conversationId: ConversationId,
        seekTo: Int = 0
    ) {
        when (val result = getMessageAsset(conversationId, messageId).await()) {
            is MessageAssetResult.Success -> {
                with(mediaPlayer) {
                    audioSnapshots[currentlyPlayedMessageId!!] = AudioSnapShot(currentPosition)

                    reset()

                    setDataSource(context, Uri.parse(result.decodedAssetPath.toString()))
                    prepare()
                    seekTo(seekTo)
                    start()

                    currentlyPlayedMessageId = messageId
                }
            }

            is MessageAssetResult.Failure -> {

            }
        }

    }

    fun close() {
        mediaPlayer.release()
        audioMessageHistory.clear()
    }

}

data class AudioState(
    var audioMediaPlayerState: AudioMediaPlayerState
)

data class AudioSnapShot(val timeStamp: Int)

sealed class AudioMediaPlayerState {
    object Playing : AudioMediaPlayerState()
    data class Stopped(val timeStamp: Int) : AudioMediaPlayerState()

    object Completed : AudioMediaPlayerState()

    object Paused : AudioMediaPlayerState()
}
