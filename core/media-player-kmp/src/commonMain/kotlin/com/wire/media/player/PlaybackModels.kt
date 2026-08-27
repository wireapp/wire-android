/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.media.player

import okio.Path

sealed interface PlaybackSource {
    data class Local(val path: Path) : PlaybackSource
    data class Remote(val location: String) : PlaybackSource
}

@Suppress("MagicNumber")
enum class PlaybackSpeed(val value: Float) {
    NORMAL(1f),
    FAST(1.5f),
    MAX(2f);

    fun toggle(): PlaybackSpeed = when (this) {
        NORMAL -> FAST
        FAST -> MAX
        MAX -> NORMAL
    }

    companion object {
        fun fromFloat(speed: Float): PlaybackSpeed = when {
            speed < FAST.value -> NORMAL
            speed < MAX.value -> FAST
            else -> MAX
        }
    }
}

data class PlaybackState(
    val isPrepared: Boolean = false,
    val isPlaying: Boolean = false,
    val isStarted: Boolean = false,
    val isCompleted: Boolean = false,
    val isBuffering: Boolean = false,
    val isMuted: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val speed: PlaybackSpeed = PlaybackSpeed.NORMAL,
    val failureReason: String? = null,
    val isReleased: Boolean = false,
)

data class PlaybackSnapshot(
    val currentPositionMs: Int,
    val durationMs: Int,
)

sealed interface PlaybackCommand {
    data class Prepare(
        val source: PlaybackSource,
        val restoredPositionMs: Int = 0,
        val playWhenReady: Boolean = false,
    ) : PlaybackCommand

    data object Play : PlaybackCommand
    data object Pause : PlaybackCommand
    data object Stop : PlaybackCommand
    data class SeekTo(val positionMs: Int) : PlaybackCommand
    data class SetMuted(val muted: Boolean) : PlaybackCommand
    data class SetSpeed(val speed: PlaybackSpeed) : PlaybackCommand
    data object Release : PlaybackCommand
}

sealed interface PlaybackEvent {
    data class Preparing(val restoredPositionMs: Int) : PlaybackEvent
    data class Ready(val durationMs: Int) : PlaybackEvent
    data class Buffering(val buffering: Boolean) : PlaybackEvent
    data object Playing : PlaybackEvent
    data object Paused : PlaybackEvent
    data object Stopped : PlaybackEvent
    data class PositionChanged(val positionMs: Int, val durationMs: Int) : PlaybackEvent
    data object Completed : PlaybackEvent
    data class MutedChanged(val muted: Boolean) : PlaybackEvent
    data class SpeedChanged(val speed: PlaybackSpeed) : PlaybackEvent
    data class Failed(val reason: String? = null) : PlaybackEvent
    data object Released : PlaybackEvent
}

sealed interface PlaybackCommandResult {
    data object Executed : PlaybackCommandResult
    data object Unsupported : PlaybackCommandResult
    data class Failure(val reason: String? = null) : PlaybackCommandResult
}

interface MediaPlaybackEngine {
    fun setEventListener(listener: ((PlaybackEvent) -> Unit)?)
    fun execute(command: PlaybackCommand): PlaybackCommandResult
    fun snapshot(): PlaybackSnapshot?
}
