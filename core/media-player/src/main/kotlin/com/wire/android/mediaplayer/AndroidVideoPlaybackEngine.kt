/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.mediaplayer

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.wire.android.di.ApplicationContext
import com.wire.media.player.MediaPlaybackEngine
import com.wire.media.player.PlaybackCommand
import com.wire.media.player.PlaybackCommandResult
import com.wire.media.player.PlaybackEvent
import com.wire.media.player.PlaybackSnapshot
import com.wire.media.player.PlaybackSource
import dev.zacsweers.metro.Inject

interface VideoPlaybackSurfaceController {
    fun attach(view: PlayerView)
    fun detach(view: PlayerView)
}

data class AndroidVideoPlaybackSession(
    val engine: MediaPlaybackEngine,
    val surfaceController: VideoPlaybackSurfaceController,
)

@Inject
class AndroidVideoPlaybackSessionFactory(
    @ApplicationContext private val context: Context,
) {
    fun create(): AndroidVideoPlaybackSession {
        val engine = AndroidVideoPlaybackEngine(context)
        return AndroidVideoPlaybackSession(engine, engine)
    }
}

private class AndroidVideoPlaybackEngine(
    context: Context,
) : MediaPlaybackEngine, VideoPlaybackSurfaceController {
    private val player = ExoPlayer.Builder(context).build()
    private var eventListener: ((PlaybackEvent) -> Unit)? = null
    private var restoredPositionMs = 0
    private var released = false

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) eventListener?.invoke(PlaybackEvent.Playing)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            eventListener?.invoke(PlaybackEvent.Buffering(playbackState == Player.STATE_BUFFERING))
            when (playbackState) {
                Player.STATE_READY -> {
                    if (restoredPositionMs > 0) {
                        player.seekTo(restoredPositionMs.toLong())
                        restoredPositionMs = 0
                    }
                    eventListener?.invoke(PlaybackEvent.Ready(player.duration.coerceAtLeast(0).toInt()))
                }
                Player.STATE_ENDED -> eventListener?.invoke(PlaybackEvent.Completed)
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            eventListener?.invoke(PlaybackEvent.Failed(error.errorCodeName))
        }
    }

    init {
        player.addListener(listener)
    }

    override fun setEventListener(listener: ((PlaybackEvent) -> Unit)?) {
        eventListener = listener
    }

    override fun execute(command: PlaybackCommand): PlaybackCommandResult = if (released) {
        PlaybackCommandResult.Failure("released")
    } else {
        when (command) {
            is PlaybackCommand.Prepare -> {
                restoredPositionMs = command.restoredPositionMs
                player.setMediaItem(MediaItem.fromUri(command.source.toAndroidUri()))
                player.prepare()
            }
            PlaybackCommand.Play -> player.play()
            PlaybackCommand.Pause -> {
                player.pause()
                eventListener?.invoke(PlaybackEvent.Paused)
            }
            PlaybackCommand.Stop -> {
                player.stop()
                eventListener?.invoke(PlaybackEvent.Stopped)
            }
            is PlaybackCommand.SeekTo -> {
                player.seekTo(command.positionMs.toLong())
                eventListener?.invoke(
                    PlaybackEvent.PositionChanged(command.positionMs, player.duration.coerceAtLeast(0).toInt())
                )
            }
            is PlaybackCommand.SetMuted -> {
                player.volume = if (command.muted) 0f else 1f
                eventListener?.invoke(PlaybackEvent.MutedChanged(command.muted))
            }
            is PlaybackCommand.SetSpeed -> {
                player.setPlaybackSpeed(command.speed.value)
                eventListener?.invoke(PlaybackEvent.SpeedChanged(command.speed))
            }
            PlaybackCommand.Release -> return release()
        }
        PlaybackCommandResult.Executed
    }

    override fun snapshot(): PlaybackSnapshot? = if (released) {
        null
    } else {
        PlaybackSnapshot(
            currentPositionMs = player.currentPosition.coerceAtLeast(0).toInt(),
            durationMs = player.duration.coerceAtLeast(0).toInt(),
        )
    }

    override fun attach(view: PlayerView) {
        view.player = player
    }

    override fun detach(view: PlayerView) {
        if (view.player === player) {
            view.player = null
        }
    }

    private fun release(): PlaybackCommandResult {
        player.removeListener(listener)
        player.release()
        released = true
        eventListener?.invoke(PlaybackEvent.Released)
        eventListener = null
        return PlaybackCommandResult.Executed
    }

    private fun PlaybackSource.toAndroidUri(): Uri = when (this) {
        is PlaybackSource.Local -> Uri.fromFile(path.toFile())
        is PlaybackSource.Remote -> Uri.parse(location)
    }
}
