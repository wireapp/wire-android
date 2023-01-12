package com.wire.android.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.zip
import javax.inject.Inject

class ConversationMessageAudioPlayer
@Inject constructor(
    private val context: Context,
    private val getMessageAsset: GetMessageAssetUseCase
) {

    private val currentAudioMessageState = MutableStateFlow<AudioMediaPlayerState>(AudioMediaPlayerState.Stopped)

    private val currentPosition = flow {
        delay(1000)
        while (true) {
            if (mediaPlayer.isPlaying) {
                emit(mediaPlayer.currentPosition)
            }
            delay(1000)
        }
    }

    private var currentAudioMessageId: String? = null

    private var audioMessageStateHistory: Map<String, AudioState> = emptyMap()

    val observableAudioMessagesState: Flow<Map<String, AudioState>> =
        currentPosition.distinctUntilChanged()
            .combine(currentAudioMessageState) { position, state ->
                audioMessageStateHistory = audioMessageStateHistory.toMutableMap().apply {
                    put(currentAudioMessageId!!, AudioState(state, position))
                }

                audioMessageStateHistory
            }

    private val mediaPlayer = MediaPlayer().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
        )
        setOnCompletionListener {

        }
    }

    suspend fun togglePlay(
        conversationId: ConversationId,
        requestedAudioMessageId: String
    ) {
        val isRequestedAudioMessageCurrentlyPlaying = currentAudioMessageId == requestedAudioMessageId

        if (isRequestedAudioMessageCurrentlyPlaying) {
            toggleCurrentlyPlayingAudioMessage()
        } else {
            stopPlayingCurrentAudioMessage()

            val previouslySavedPositionsOrNull = audioMessageStateHistory[requestedAudioMessageId]?.currentPosition

            playAudioMessage(
                conversationId = conversationId,
                messageId = requestedAudioMessageId,
                seekTo = previouslySavedPositionsOrNull
            )
        }
    }

private fun toggleCurrentlyPlayingAudioMessage() {
    if (mediaPlayer.isPlaying) {
        pause()
    } else {
        resumeAudio()
    }
}

private suspend fun playAudioMessage(
    conversationId: ConversationId,
    messageId: String,
    seekTo: Int? = null
) {
    when (val result = getMessageAsset(conversationId, messageId).await()) {
        is MessageAssetResult.Success -> {
            mediaPlayer.setDataSource(
                context,
                Uri.parse(result.decodedAssetPath.toString())
            )
            mediaPlayer.prepare()

            if (seekTo != null) {
                mediaPlayer.seekTo(seekTo)
            }

            mediaPlayer.start()

            currentAudioMessageId = messageId

            currentAudioMessageState.value = AudioMediaPlayerState.Playing
        }

        is MessageAssetResult.Failure -> {

        }
    }
}

private fun resumeAudio() {
    currentAudioMessageState.value = AudioMediaPlayerState.Playing

    mediaPlayer.start()
}

private fun pause() {
    currentAudioMessageState.value = AudioMediaPlayerState.Paused

    mediaPlayer.pause()
}

private fun stopPlayingCurrentAudioMessage() {
    currentAudioMessageState.value = AudioMediaPlayerState.Stopped

    mediaPlayer.reset()
}

fun close() {
    mediaPlayer.release()
}

}

data class AudioState(
    val audioMediaPlayerState: AudioMediaPlayerState,
    val currentPosition: Int
)

sealed class AudioMediaPlayerState {
    object Playing : AudioMediaPlayerState()
    object Stopped : AudioMediaPlayerState()

    object Completed : AudioMediaPlayerState()

    object Paused : AudioMediaPlayerState()
}
