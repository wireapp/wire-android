package com.wire.android.media.audiomessage

sealed class AudioMediaPlayingState {
    object Playing : AudioMediaPlayingState()
    object Stopped : AudioMediaPlayingState()

    object Completed : AudioMediaPlayingState()

    object Paused : AudioMediaPlayingState()

    object Fetching : AudioMediaPlayingState()

    object SuccessFullFetching : AudioMediaPlayingState()

    object Failed : AudioMediaPlayingState()
}
