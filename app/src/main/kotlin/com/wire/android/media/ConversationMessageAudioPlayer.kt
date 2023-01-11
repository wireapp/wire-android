package com.wire.android.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

class ConversationMessageAudioPlayer
@Inject constructor(
    private val context: Context,
    private val getMessageAsset: GetMessageAssetUseCase
) {

    val audioMessagesState: SnapshotStateMap<String, AudioState> by mutableStateMapOf()

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
            audioSnapshots.remove(currentlyPlayedMessageId)
        }
    }

    suspend fun togglePlay(
        conversationId: ConversationId,
        messageId: String
    ) {
        val switchAudioMessage = currentlyPlayedMessageId != messageId

        if (switchAudioMessage) {
            switchAudioMessage(
                messageId = messageId,
                conversationId = conversationId
            )
        } else {
            playOrPauseCurrentAudio()
        }
    }

    private fun playOrPauseCurrentAudio() {
        if (mediaPlayer.isPlaying) {
            pause()
        } else {
            start()
        }
    }

    private fun pause() {
        updateOrPutAudioState(
            audioMediaPlayerState = AudioMediaPlayerState.Paused
        )
        mediaPlayer.pause()
    }

    private fun start() {
        updateOrPutAudioState(
            audioMediaPlayerState = AudioMediaPlayerState.Playing
        )
        mediaPlayer.start()
    }

    private fun reset() {
        mediaPlayer.reset()
    }

    private suspend fun switchAudioMessage(
        messageId: String,
        conversationId: ConversationId
    ) {
        when (val result = getMessageAsset(conversationId, messageId).await()) {
            is MessageAssetResult.Success -> {
                reset()

                mediaPlayer.setDataSource(context, Uri.parse(result.decodedAssetPath.toString()))
                mediaPlayer.prepare()

                val isInitialPlay = currentlyPlayedMessageId == null

                val audioSnapShot = audioSnapshots[messageId]
                val isSongPlayedInThePast = audioSnapShot != null

                if (!isInitialPlay) {
                    saveCurrentAudioMessageSnapShot()
                    if (isSongPlayedInThePast) {
                        mediaPlayer.seekTo(audioSnapShot!!.timeStamp)
                    }
                }

                currentlyPlayedMessageId = messageId

                start()
            }

            is MessageAssetResult.Failure -> {

            }
        }
    }

    private fun saveCurrentAudioMessageSnapShot() {
        audioSnapshots[currentlyPlayedMessageId!!] = AudioSnapShot(mediaPlayer.currentPosition)
    }

    private fun updateOrPutAudioState(audioMediaPlayerState: AudioMediaPlayerState) {
        audioMessagesState[currentlyPlayedMessageId!!] = AudioState(audioMediaPlayerState = audioMediaPlayerState)
    }

    fun close() {
        mediaPlayer.release()
    }

}

data class AudioState(
    val audioMediaPlayerState: AudioMediaPlayerState
)

data class AudioSnapShot(val timeStamp: Int)

sealed class AudioMediaPlayerState {
    object Playing : AudioMediaPlayerState()
    data class Stopped(val timeStamp: Int) : AudioMediaPlayerState()

    object Completed : AudioMediaPlayerState()

    object Paused : AudioMediaPlayerState()
}
