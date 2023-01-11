package com.wire.android.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class ConversationMessageAudioPlayer
@Inject constructor(
    private val context: Context,
    private val getMessageAsset: GetMessageAssetUseCase
) {

    val audioMessageHistory = MutableStateFlow(mutableMapOf<String, AudioState>())

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
            updateOrPutAudioState(
                audioMediaPlayerState = AudioMediaPlayerState.Playing
            )
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

    }

    private suspend fun switchAudioMessage(
        messageId: String,
        conversationId: ConversationId
    ) {
        when (val result = getMessageAsset(conversationId, messageId).await()) {
            is MessageAssetResult.Success -> {
                with(mediaPlayer) {
                    val isInitialPlay = currentlyPlayedMessageId == null

                    if (!isInitialPlay) {
                        saveCurrentAudioMessageSnapShot()
                    }

                    reset()
                    setDataSource(context, Uri.parse(result.decodedAssetPath.toString()))
                    prepare()

                    val audioSnapShot = audioSnapshots[messageId]
                    val isSongPlayedInThePast = audioSnapShot != null

                    if (isSongPlayedInThePast) {
                        seekTo(audioSnapShot!!.timeStamp)
                    }

                    start()

                    currentlyPlayedMessageId = messageId
                }
            }

            is MessageAssetResult.Failure -> {

            }
        }
    }

    private fun saveCurrentAudioMessageSnapShot() {
        audioSnapshots[currentlyPlayedMessageId!!] = AudioSnapShot(mediaPlayer.currentPosition)
    }

    fun close() {
        mediaPlayer.release()
    }

    private fun updateOrPutAudioState(audioMediaPlayerState: AudioMediaPlayerState) {
        audioMessageHistory.update { audioMessageHistory ->
            audioMessageHistory[currentlyPlayedMessageId!!] = AudioState(audioMediaPlayerState)
            audioMessageHistory
        }
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
