package com.wire.android.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ConversationMessageAudioPlayer
@Inject constructor(
    private val context: Context,
    private val getMessageAsset: GetMessageAssetUseCase
) {

    private val audioMessageHistory = mutableMapOf<String, AudioState>()

    private val audioSnapshots = mutableMapOf<String, AudioSnapShot>()

    private var currentlyPlayedMessageId: String? = null

    private var _audioMediaPlayerState: MutableStateFlow<AudioMediaPlayerState> = MutableStateFlow(
        AudioMediaPlayerState.Paused
    )

    private val mediaPlayer = MediaPlayer().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
        )
        setOnCompletionListener {
            currentlyPlayedMessageId = null
        }
    }

    val currentProgress = flow {
        delay(1000)
        while (true) {
            emit(mediaPlayer.currentPosition)
            delay(1000)
        }
    }

    val audioMediaPlayerState = _audioMediaPlayerState.map {
        _audioMediaPlayerState.value to currentlyPlayedMessageId
    }

    suspend fun togglePlay(
        conversationId: ConversationId,
        messageId: String
    ) {
        with(mediaPlayer) {
            val isInitialPlay = currentlyPlayedMessageId == null

            if (isInitialPlay) {
                when (val result = getMessageAsset(conversationId, messageId).await()) {
                    is MessageAssetResult.Success -> {
                        reset()

                        setDataSource(context, Uri.parse(result.decodedAssetPath.toString()))
                        prepare()
                        start()

                        currentlyPlayedMessageId = messageId
                    }

                    is MessageAssetResult.Failure -> {
                        println("error")
                    }
                }
            } else {
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
                        when (val result = getMessageAsset(conversationId, messageId).await()) {
                            is MessageAssetResult.Success -> {
                                audioSnapshots[currentlyPlayedMessageId!!] = AudioSnapShot(currentPosition)

                                reset()

                                setDataSource(context, Uri.parse(result.decodedAssetPath.toString()))
                                prepare()
                                seekTo(audioSnapShot!!.timeStamp)
                                start()

                                currentlyPlayedMessageId = messageId
                            }

                            is MessageAssetResult.Failure -> {

                            }
                        }
                    } else {
                        when (val result = getMessageAsset(conversationId, messageId).await()) {
                            is MessageAssetResult.Success -> {
                                audioSnapshots[currentlyPlayedMessageId!!] = AudioSnapShot(currentPosition)

                                reset()

                                setDataSource(context, Uri.parse(result.decodedAssetPath.toString()))
                                prepare()
                                start()

                                currentlyPlayedMessageId = messageId
                            }

                            is MessageAssetResult.Failure -> {
                                println("error")
                            }
                        }
                    }
                }
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
