package com.wire.android.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

class ConversationMessageAudioPlayer
@Inject constructor(
    private val context: Context,
    private val getMessageAsset: GetMessageAssetUseCase
) {

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

    suspend fun play(conversationId: ConversationId, messageId: String) {
        if (_audioMediaPlayerState.value is AudioMediaPlayerState.Playing) {
            stop()
        }

        currentlyPlayedMessageId = messageId

        when (val result = getMessageAsset(conversationId, messageId).await()) {
            is MessageAssetResult.Success -> {
                with(mediaPlayer) {
                    reset()
                    setDataSource(context, Uri.parse(result.decodedAssetPath.toString()))
                    prepare()
                    start()

                    _audioMediaPlayerState.value = AudioMediaPlayerState.Playing
                }
            }

            is MessageAssetResult.Failure -> {

            }
        }
    }

    fun pause() {
        with(mediaPlayer) {
            if (isPlaying) {
                pause()

                _audioMediaPlayerState.value = AudioMediaPlayerState.Paused
            }
        }
    }

    fun stop() {
        with(mediaPlayer) {
            if (isPlaying) {
                stop()

                _audioMediaPlayerState.value = AudioMediaPlayerState.Stopped
            }
            release()
        }
    }

    fun close() {
        mediaPlayer.release()

        _audioMediaPlayerState.value = AudioMediaPlayerState.Paused
    }

}

sealed class AudioMediaPlayerState {
    object Playing : AudioMediaPlayerState()
    object Paused : AudioMediaPlayerState()
    object Stopped : AudioMediaPlayerState()
}
