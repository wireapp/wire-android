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
            reset()

            _audioMediaPlayerState.value = AudioMediaPlayerState.Paused
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
        val existingAudioState = audioMessageHistory[messageId]

        with(mediaPlayer) {
            if (existingAudioState != null) {

                when (val mediaPlayerState = existingAudioState.audioMediaPlayerState) {
                    is AudioMediaPlayerState.Playing -> {
                        pause()
                    }

                    AudioMediaPlayerState.Paused -> {
                        start()
                    }

                    is AudioMediaPlayerState.Stopped -> {
                        when (val result = getMessageAsset(conversationId, messageId).await()) {
                            is MessageAssetResult.Success -> {

                                reset()

                                setDataSource(context, Uri.parse(result.decodedAssetPath.toString()))
                                prepare()
                                seekTo(mediaPlayerState.timeStamp)
                                start()

                                _audioMediaPlayerState.value = AudioMediaPlayerState.Playing
                            }

                            is MessageAssetResult.Failure -> {

                            }
                        }
                    }
                }
            } else {
                when (val result = getMessageAsset(conversationId, messageId).await()) {
                    is MessageAssetResult.Success -> {

                        reset()

                        setDataSource(context, Uri.parse(result.decodedAssetPath.toString()))
                        prepare()
                        start()

                        _audioMediaPlayerState.value = AudioMediaPlayerState.Playing
                    }

                    is MessageAssetResult.Failure -> {

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
    var audioMediaPlayerState: AudioMediaPlayerState,
)

sealed class AudioMediaPlayerState {
    object Playing : AudioMediaPlayerState()
    data class Stopped(val timeStamp: Int) : AudioMediaPlayerState()

    object Paused : AudioMediaPlayerState()
}
