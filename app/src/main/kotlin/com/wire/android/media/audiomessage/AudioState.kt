package com.wire.android.media.audiomessage

data class AudioState(
    val audioMediaPlayingState: AudioMediaPlayingState,
    val currentPositionInMs: Int,
    val totalTimeInMs: TotalTimeInMs
) {
    companion object {
        val DEFAULT = AudioState(AudioMediaPlayingState.Stopped, 0, TotalTimeInMs.NotKnown)
    }

    // before the user decides to play audio message, we are not able to determine total time ourself using
    // MediaPlayer API, we are relying on the info retrieved from the other client, until then
    fun adjustTotalTime(otherClientTotalTime: Int): AudioState {
        if (totalTimeInMs is TotalTimeInMs.NotKnown) {
            if (otherClientTotalTime != 0) {
                return copy(totalTimeInMs = TotalTimeInMs.Known(otherClientTotalTime))
            }
        }

        return this
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
