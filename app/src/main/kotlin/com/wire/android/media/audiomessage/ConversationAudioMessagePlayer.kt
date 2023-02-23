package com.wire.android.media.audiomessage

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import javax.inject.Inject

class ConversationAudioMessagePlayer
@Inject constructor(
    private val context: Context,
    private val audioMediaPlayer: MediaPlayer,
    private val getMessageAsset: GetMessageAssetUseCase
) {
    private companion object {
        const val UPDATE_POSITION_INTERVAL_IN_MS = 1000L
    }

    init {
        audioMediaPlayer.setOnCompletionListener {
            if (currentAudioMessageId != null) {
                audioMessageStateUpdate.tryEmit(
                    AudioMediaPlayerStateUpdate.AudioMediaPlayingStateUpdate(
                        currentAudioMessageId!!,
                        AudioMediaPlayingState.Completed
                    )
                )
                seekToAudioPosition.tryEmit(currentAudioMessageId!! to 0)
            }
        }
    }

    private val audioMessageStateUpdate =
        MutableSharedFlow<AudioMediaPlayerStateUpdate>(
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
            extraBufferCapacity = 1
        )

    private val mediaPlayerPosition = flow {
        delay(UPDATE_POSITION_INTERVAL_IN_MS)
        while (true) {
            if (audioMediaPlayer.isPlaying) {
                emit(currentAudioMessageId to audioMediaPlayer.currentPosition)
            }
            delay(UPDATE_POSITION_INTERVAL_IN_MS)
        }
    }.distinctUntilChanged()

    private val seekToAudioPosition =
        MutableSharedFlow<Pair<String, Int>>(
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
            extraBufferCapacity = 1
        )

    private val positionChangedUpdate = merge(mediaPlayerPosition, seekToAudioPosition)
        .map { (messageId, position) ->
            messageId?.let {
                AudioMediaPlayerStateUpdate.PositionChangeUpdate(it, position)
            }
        }.filterNotNull()

    private var audioMessageStateHistory: Map<String, AudioState> = emptyMap()

    // Flow collecting the audio message state updates as well as the audio message position
    // updates, the collected values are then put into the map holding the state for each individual audio message.
    // The audio message position can be either updated by the user manually by for example a Slider component or by the player itself.
    val observableAudioMessagesState: Flow<Map<String, AudioState>> =
        merge(positionChangedUpdate, audioMessageStateUpdate).map { audioMessageStateUpdate ->
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
                            currentState.copy(totalTimeInMs = audioMessageStateUpdate.totalTimeInMs)
                        )
                    }
                }
            }

            audioMessageStateHistory
        }

    private var currentAudioMessageId: String? = null

    suspend fun playAudio(
        conversationId: ConversationId,
        requestedAudioMessageId: String
    ) {
        val isRequestedAudioMessageCurrentlyPlaying = currentAudioMessageId == requestedAudioMessageId

        if (isRequestedAudioMessageCurrentlyPlaying) {
            toggleAudioMessage(requestedAudioMessageId)
        } else {
            if (currentAudioMessageId != null) {
                val currentAudioState = audioMessageStateHistory[currentAudioMessageId]
                if (currentAudioState?.audioMediaPlayingState != AudioMediaPlayingState.Fetching) {
                    stop(currentAudioMessageId!!)
                }
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
        if (audioMediaPlayer.isPlaying) {
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

        coroutineScope {
            launch {
                audioMessageStateUpdate.emit(
                    AudioMediaPlayerStateUpdate.AudioMediaPlayingStateUpdate(
                        messageId,
                        AudioMediaPlayingState.Fetching
                    )
                )

                when (val result = getMessageAsset(conversationId, messageId).await()) {
                    is MessageAssetResult.Success -> {
                        audioMessageStateUpdate.emit(
                            AudioMediaPlayerStateUpdate.AudioMediaPlayingStateUpdate(
                                messageId,
                                AudioMediaPlayingState.SuccessfulFetching
                            )
                        )

                        val isFetchedAudioCurrentlyQueuedToPlay = messageId == currentAudioMessageId

                        if (isFetchedAudioCurrentlyQueuedToPlay) {
                            audioMediaPlayer.setDataSource(
                                context,
                                Uri.parse(result.decodedAssetPath.toString())
                            )
                            audioMediaPlayer.prepare()

                            if (position != null) {
                                audioMediaPlayer.seekTo(position)
                            }

                            audioMediaPlayer.start()

                            audioMessageStateUpdate.emit(
                                AudioMediaPlayerStateUpdate.AudioMediaPlayingStateUpdate(
                                    messageId,
                                    AudioMediaPlayingState.Playing
                                )
                            )

                            audioMessageStateUpdate.emit(
                                AudioMediaPlayerStateUpdate.TotalTimeUpdate(
                                    messageId,
                                    audioMediaPlayer.duration
                                )
                            )
                        }
                    }

                    is MessageAssetResult.Failure -> {
                        audioMessageStateUpdate.emit(
                            AudioMediaPlayerStateUpdate.AudioMediaPlayingStateUpdate(
                                messageId,
                                AudioMediaPlayingState.Failed
                            )
                        )
                    }
                }
            }
        }
    }

    suspend fun setPosition(messageId: String, position: Int) {
        val currentAudioState = audioMessageStateHistory[messageId]

        if (currentAudioState != null) {
            val isAudioMessageCurrentlyPlaying = currentAudioMessageId == messageId

            if (isAudioMessageCurrentlyPlaying) {
                audioMediaPlayer.seekTo(position)
            }
        }

        seekToAudioPosition.emit(messageId to position)
    }

    private suspend fun resumeAudio(messageId: String) {
        audioMediaPlayer.start()

        audioMessageStateUpdate.emit(
            AudioMediaPlayerStateUpdate.AudioMediaPlayingStateUpdate(messageId, AudioMediaPlayingState.Playing)
        )
    }

    private suspend fun pause(messageId: String) {
        audioMediaPlayer.pause()

        audioMessageStateUpdate.emit(
            AudioMediaPlayerStateUpdate.AudioMediaPlayingStateUpdate(messageId, AudioMediaPlayingState.Paused)
        )
    }

    private suspend fun stop(messageId: String) {
        audioMediaPlayer.reset()

        audioMessageStateUpdate.emit(
            AudioMediaPlayerStateUpdate.AudioMediaPlayingStateUpdate(messageId, AudioMediaPlayingState.Stopped)
        )
    }

    fun close() {
        audioMediaPlayer.release()
    }
}
