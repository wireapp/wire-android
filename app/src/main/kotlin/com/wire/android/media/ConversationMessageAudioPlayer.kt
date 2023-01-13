package com.wire.android.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.wire.android.util.CurrentScreenManager
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import javax.inject.Inject

class ConversationMessageAudioPlayer
@Inject constructor(
    private val context: Context,
    private val getMessageAsset: GetMessageAssetUseCase,
) {

    private val audioMessageStateUpdate = MutableSharedFlow<AudioMediaPlayerStateUpdate.AudioMediaPlayingStateUpdate>()

    private val mediaPlayerPosition = flow {
        delay(1)
        while (true) {
            if (mediaPlayer.isPlaying) {
                emit(mediaPlayer.currentPosition)
            }
            delay(1)
        }
    }.distinctUntilChanged()

    private val seekToAudioPosition = MutableSharedFlow<Int>()

    private val positionChangedUpdate = flowOf(mediaPlayerPosition, seekToAudioPosition)
        .flattenConcat()
        .map { position ->
            currentAudioMessageId?.let {
                AudioMediaPlayerStateUpdate.PositionChangeUpdate(it, position)
            }
        }.filterNotNull()

    private var audioMessageStateHistory: Map<String, AudioState> = emptyMap()

    val observableAudioMessagesState: Flow<Map<String, AudioState>> =
        merge(positionChangedUpdate, audioMessageStateUpdate).map { audioMessageStateUpdate ->
            val currentState = audioMessageStateHistory.getOrDefault(
                audioMessageStateUpdate.messageId,
                AudioState(AudioMediaPlayingState.Paused, 0)
            )

            when (audioMessageStateUpdate) {
                is AudioMediaPlayerStateUpdate.AudioMediaPlayingStateUpdate -> {
                    audioMessageStateHistory = audioMessageStateHistory.toMutableMap().apply {
                        put(
                            audioMessageStateUpdate.messageId,
                            currentState.copy(audioMediaPlayingState = audioMessageStateUpdate.audioMediaPlayingState)
                        )
                    }
                }

                is AudioMediaPlayerStateUpdate.PositionChangeUpdate -> {
                    audioMessageStateHistory = audioMessageStateHistory.toMutableMap().apply {
                        put(
                            audioMessageStateUpdate.messageId,
                            currentState.copy(currentPosition = audioMessageStateUpdate.position)
                        )
                    }
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
            audioMessageStateUpdate.tryEmit(
                AudioMediaPlayerStateUpdate.AudioMediaPlayingStateUpdate(
                    currentAudioMessageId!!,
                    AudioMediaPlayingState.Completed
                )
            )
        }
    }

    suspend fun playAudio(
        conversationId: ConversationId,
        requestedAudioMessageId: String
    ) {
        val isRequestedAudioMessageCurrentlyPlaying = currentAudioMessageId == requestedAudioMessageId

        if (isRequestedAudioMessageCurrentlyPlaying) {
            toggleAudioMessage(requestedAudioMessageId)
        } else {
            if (currentAudioMessageId != null) {
                stop(currentAudioMessageId!!)
            }

            val previouslySavedPositionOrNull = audioMessageStateHistory[requestedAudioMessageId]?.run {
                if (audioMediaPlayingState == AudioMediaPlayingState.Completed) {
                    0
                } else {
                    currentPosition
                }
            }

            playAudioMessage(
                conversationId = conversationId,
                messageId = requestedAudioMessageId,
                position = previouslySavedPositionOrNull
            )
        }
    }

    private suspend fun toggleAudioMessage(messageId: String) {
        if (mediaPlayer.isPlaying) {
            pause(messageId)
        } else {
            resumeAudio(messageId)
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

                audioMessageStateUpdate.emit(
                    AudioMediaPlayerStateUpdate.AudioMediaPlayingStateUpdate(
                        messageId,
                        AudioMediaPlayingState.Playing
                    )
                )
            }

            is MessageAssetResult.Failure -> {

            }
        }
    }

    private suspend fun seekTo(position: Int) {
        mediaPlayer.seekTo(position)
        seekToAudioPosition.emit(position)
    }

    private suspend fun resumeAudio(messageId: String) {
        mediaPlayer.start()
        audioMessageStateUpdate.emit(
            AudioMediaPlayerStateUpdate.AudioMediaPlayingStateUpdate(messageId, AudioMediaPlayingState.Playing)
        )
    }

    private suspend fun pause(messageId: String) {
        mediaPlayer.pause()
        audioMessageStateUpdate.emit(
            AudioMediaPlayerStateUpdate.AudioMediaPlayingStateUpdate(messageId, AudioMediaPlayingState.Paused)
        )
    }

    private suspend fun stop(messageId: String) {
        mediaPlayer.reset()
        audioMessageStateUpdate.emit(
            AudioMediaPlayerStateUpdate.AudioMediaPlayingStateUpdate(messageId, AudioMediaPlayingState.Stopped)
        )
    }

    fun close() {
        mediaPlayer.release()
    }

}

data class AudioState(
    val audioMediaPlayingState: AudioMediaPlayingState,
    val currentPosition: Int
)

sealed class AudioMediaPlayingState {
    object Playing : AudioMediaPlayingState()
    object Stopped : AudioMediaPlayingState()

    object Completed : AudioMediaPlayingState()

    object Paused : AudioMediaPlayingState()
}

sealed class AudioMediaPlayerStateUpdate(
    open val messageId: String
) {
    data class AudioMediaPlayingStateUpdate(
        override val messageId: String,
        val audioMediaPlayingState: AudioMediaPlayingState
    ) : AudioMediaPlayerStateUpdate(messageId)

    data class PositionChangeUpdate(
        override val messageId: String,
        val position: Int
    ) : AudioMediaPlayerStateUpdate(messageId)
}
