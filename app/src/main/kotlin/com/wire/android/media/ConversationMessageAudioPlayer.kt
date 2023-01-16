package com.wire.android.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import javax.inject.Inject

class ConversationMessageAudioPlayer
@Inject constructor(
    private val context: Context,
    private val getMessageAsset: GetMessageAssetUseCase,
) {
    private companion object {
        const val UPDATE_POSITION_INTERVAL_IN_MS = 1L
    }

    private val audioMessageStateUpdate = MutableSharedFlow<AudioMediaPlayerStateUpdate>()

    private val mediaPlayerPosition = flow {
        delay(UPDATE_POSITION_INTERVAL_IN_MS)
        while (true) {
            if (mediaPlayer.isPlaying) {
                emit(currentAudioMessageId to mediaPlayer.currentPosition)
            }
            delay(UPDATE_POSITION_INTERVAL_IN_MS)
        }
    }.distinctUntilChanged()

    private val seekToAudioPosition = MutableSharedFlow<Pair<String, Int>>()

    private val positionChangedUpdate = merge(mediaPlayerPosition, seekToAudioPosition)
        .map { (messageId, position) ->
            messageId?.let {
                AudioMediaPlayerStateUpdate.PositionChangeUpdate(it, position)
            }
        }.filterNotNull()

    private var audioMessageStateHistory: Map<String, AudioState> = emptyMap()

    val observableAudioMessagesState: Flow<Map<String, AudioState>> =
        merge(positionChangedUpdate, audioMessageStateUpdate).map { audioMessageStateUpdate ->
            Log.d("TEST", "audioMessageStateUpdate $audioMessageStateUpdate")
            val currentState = audioMessageStateHistory.getOrDefault(
                audioMessageStateUpdate.messageId,
                AudioState(AudioMediaPlayingState.Paused, 0, 0)
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
                            currentState.copy(currentPositionInMs = audioMessageStateUpdate.position)
                        )
                    }
                }

                is AudioMediaPlayerStateUpdate.TotalTimeUpdate -> {
                    audioMessageStateHistory = audioMessageStateHistory.toMutableMap().apply {
                        put(
                            audioMessageStateUpdate.messageId,
                            currentState.copy(totalTimeInMs = audioMessageStateUpdate.totalTime)
                        )
                    }
                    delay(1000)
                }
            }

            audioMessageStateHistory
        }

    private fun updateAudioMessageStateHistory() {

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
            if (currentAudioMessageId != null) {
                audioMessageStateUpdate.tryEmit(
                    AudioMediaPlayerStateUpdate.AudioMediaPlayingStateUpdate(
                        currentAudioMessageId!!,
                        AudioMediaPlayingState.Completed
                    )
                )
            }
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
                    currentPositionInMs
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
                    mediaPlayer.seekTo(position)
                }

                audioMessageStateUpdate.emit(
                    AudioMediaPlayerStateUpdate.TotalTimeUpdate(
                        messageId,
                        mediaPlayer.duration
                    )
                )

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

    suspend fun setPosition(messageId: String, position: Int) {
        val currentAudioState = audioMessageStateHistory[messageId]

        if (currentAudioState != null) {
            val isAudioMessageCurrentlyPlaying = currentAudioMessageId == messageId

            if (isAudioMessageCurrentlyPlaying) {
                mediaPlayer.seekTo(position)
            }
        }

        seekToAudioPosition.emit(messageId to position)
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
    val totalTimeInMs: Int,
    val currentPositionInMs: Int
) {
    companion object {
        val DEFAULT = AudioState(AudioMediaPlayingState.Paused, 0, 0)
    }

    private val totalTimeInSec = totalTimeInMs / 1000

    private val currentPositionInSec = currentPositionInMs / 1000
    val formattedTimeLeft
        get() = run {
            val timeLeft = (totalTimeInSec - currentPositionInSec)

            val minutes = timeLeft / 60
            val seconds = timeLeft % 60
            val formattedSeconds = String.format("%02d", seconds)

            "$minutes:$formattedSeconds"
        }
}

sealed class AudioMediaPlayingState {
    object Playing : AudioMediaPlayingState()
    object Stopped : AudioMediaPlayingState()

    object Completed : AudioMediaPlayingState()

    object Paused : AudioMediaPlayingState()
}

private sealed class AudioMediaPlayerStateUpdate(
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

    data class TotalTimeUpdate(
        override val messageId: String,
        val totalTime: Int
    ) : AudioMediaPlayerStateUpdate(messageId)
}
