package com.wire.android.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.zip
import javax.inject.Inject

class ConversationMessageAudioPlayer
@Inject constructor(
    private val context: Context,
    private val getMessageAsset: GetMessageAssetUseCase
) {

    private val currentAudioMessageState = MutableSharedFlow<AudioMediaPlayerState>()

    private val mediaPlayerPosition = flow {
        delay(1000)
        while (true) {
            if (mediaPlayer.isPlaying) {
                emit(mediaPlayer.currentPosition)
            }
            delay(1000)
        }
    }.distinctUntilChanged()

    private val seekToAudioPosition = MutableSharedFlow<Int>()

    private val currentAudioPosition = flowOf(mediaPlayerPosition, seekToAudioPosition).flattenConcat()

    private var audioMessageStateHistory: Map<String, AudioState> = emptyMap()

    val observableAudioMessagesState: Flow<Map<String, AudioState>> =
        currentAudioMessageState
            .combine(currentAudioPosition) { state, position ->
                if (currentAudioMessageId != null) {
                    audioMessageStateHistory = audioMessageStateHistory.toMutableMap().apply {
                        put(currentAudioMessageId!!, AudioState(state, position))
                    }
                }

                audioMessageStateHistory
            }

    private var currentAudioMessageId: String? = null

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
            if (currentAudioMessageId != null) {
                stopPlayingCurrentAudioMessage()
            }

            val previouslySavedPositionsOrNull = audioMessageStateHistory[requestedAudioMessageId]?.currentPosition

            playAudioMessage(
                conversationId = conversationId,
                messageId = requestedAudioMessageId,
                position = previouslySavedPositionsOrNull
            )
        }
    }

    private suspend fun toggleCurrentlyPlayingAudioMessage() {
        if (mediaPlayer.isPlaying) {
            pause()
        } else {
            resumeAudio()
        }
    }

    private suspend fun playAudioMessage(
        conversationId: ConversationId,
        messageId: String,
        position: Int? = null
    ) {
        currentAudioMessageId = messageId

        when (val result = getMessageAsset(conversationId, messageId).await()) {
            is MessageAssetResult.Success -> {
                mediaPlayer.setDataSource(
                    context,
                    Uri.parse(result.decodedAssetPath.toString())
                )
                mediaPlayer.prepare()

                if (position != null) {
                    seekTo(position)
                }

                mediaPlayer.start()

                currentAudioMessageState.emit(AudioMediaPlayerState.Playing)
            }

            is MessageAssetResult.Failure -> {

            }
        }
    }

    private suspend fun seekTo(position: Int) {
        mediaPlayer.seekTo(position)

        seekToAudioPosition.emit(position)
    }

    private suspend fun resumeAudio() {
        mediaPlayer.start()

        currentAudioMessageState.emit(AudioMediaPlayerState.Playing)
    }

    private suspend fun pause() {
        mediaPlayer.pause()

        currentAudioMessageState.emit(AudioMediaPlayerState.Paused)
    }

    private suspend fun stopPlayingCurrentAudioMessage() {
        mediaPlayer.reset()

        currentAudioMessageState.emit(AudioMediaPlayerState.Stopped)
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
