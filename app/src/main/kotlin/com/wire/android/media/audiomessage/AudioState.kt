package com.wire.android.media.audiomessage

data class AudioState(
    val audioMediaPlayingState: AudioMediaPlayingState,
    val currentPositionInMs: Int,
    val totalTimeInMs: TotalTimeInMs
) {
    companion object {
        val DEFAULT = AudioState(AudioMediaPlayingState.Stopped, 0, TotalTimeInMs.NotKnown)
        fun withTotalTimeFromOtherClient(otherClientTotalTime: Int): AudioState {
            val totalTimeInMs = if (otherClientTotalTime == 0) {
                TotalTimeInMs.NotKnown
            } else {
                TotalTimeInMs.Known(otherClientTotalTime)
            }

            return DEFAULT.copy(totalTimeInMs = totalTimeInMs)
        }
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
