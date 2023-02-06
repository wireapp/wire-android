package com.wire.android.media.audiomessage

data class AudioState(
    val audioMediaPlayingState: AudioMediaPlayingState,
    val currentPositionInMs: Int,
    val totalTimeInMs: Int
) {
    companion object {
        val DEFAULT = AudioState(AudioMediaPlayingState.Paused, 0, 0)
    }
}
