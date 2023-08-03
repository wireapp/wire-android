package com.wire.android.media.audiomessage

data class AudioState(
    val audioMediaPlayingState: AudioMediaPlayingState,
    val currentPositionInMs: Int,
    val totalTimeInMs: TotalTimeInMs
) {
    companion object {
        val DEFAULT = AudioState(AudioMediaPlayingState.Stopped, 0, TotalTimeInMs.NotKnown)
    }

    // if the back-end returned the total time, we use that, in case it didn't we use what we get from
    // the [ConversationAudioMessagePlayer.kt] which will emit the time once the users play the audio.
    fun sanitizeTotalTime(otherClientTotalTime: Int): TotalTimeInMs {
        if (otherClientTotalTime != 0) {
           return TotalTimeInMs.Known(otherClientTotalTime)
        }

        return totalTimeInMs
    }

    sealed class TotalTimeInMs {
        object NotKnown : TotalTimeInMs()

        data class Known(val value: Int) : TotalTimeInMs()
    }
}

sealed class AudioMediaPlayingState {
    object Playing : AudioMediaPlayingState()
    object Stopped : AudioMediaPlayingState()

    object Completed : AudioMediaPlayingState()

    object Paused : AudioMediaPlayingState()

    object Fetching : AudioMediaPlayingState()

    object SuccessfulFetching : AudioMediaPlayingState()

    object Failed : AudioMediaPlayingState()
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

    data class TotalTimeUpdate(
        override val messageId: String,
        val totalTimeInMs: Int
    ) : AudioMediaPlayerStateUpdate(messageId)
}

sealed class RecordAudioMediaPlayerStateUpdate {
    data class RecordAudioMediaPlayingStateUpdate(
        val audioMediaPlayingState: AudioMediaPlayingState
    ) : RecordAudioMediaPlayerStateUpdate()

    data class PositionChangeUpdate(
        val position: Int
    ) : RecordAudioMediaPlayerStateUpdate()

    data class TotalTimeUpdate(
        val totalTimeInMs: Int
    ) : RecordAudioMediaPlayerStateUpdate()
}
