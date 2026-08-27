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
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.wire.android.di.ApplicationContext
import com.wire.media.player.MediaPlaybackEngine
import com.wire.media.player.PlaybackCommand
import com.wire.media.player.PlaybackCommandResult
import com.wire.media.player.PlaybackEvent
import com.wire.media.player.PlaybackSnapshot
import com.wire.media.player.PlaybackSource
import dev.zacsweers.metro.Inject
import java.io.IOException

@Inject
class AndroidMediaPlayerPlaybackEngineFactory(
    @ApplicationContext private val context: Context,
) {
    fun create(): MediaPlaybackEngine = AndroidMediaPlayerPlaybackEngine(context)
}

private class AndroidMediaPlayerPlaybackEngine(
    private val context: Context,
) : MediaPlaybackEngine {
    private val player = MediaPlayer().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
        )
        setOnPreparedListener {
            prepared = true
            if (restoredPositionMs > 0) seekTo(restoredPositionMs)
            eventListener?.invoke(PlaybackEvent.Ready(duration.coerceAtLeast(0)))
        }
        setOnCompletionListener { eventListener?.invoke(PlaybackEvent.Completed) }
        setOnErrorListener { _, what, extra ->
            eventListener?.invoke(PlaybackEvent.Failed("media_player_error_${what}_$extra"))
            true
        }
    }

    private var eventListener: ((PlaybackEvent) -> Unit)? = null
    private var prepared = false
    private var released = false
    private var restoredPositionMs = 0

    override fun setEventListener(listener: ((PlaybackEvent) -> Unit)?) {
        eventListener = listener
    }

    override fun execute(command: PlaybackCommand): PlaybackCommandResult = try {
        executeCommand(command)
    } catch (error: IOException) {
        PlaybackCommandResult.Failure(error.message)
    } catch (error: SecurityException) {
        PlaybackCommandResult.Failure("permission_denied")
    } catch (error: IllegalArgumentException) {
        PlaybackCommandResult.Failure(error.message)
    } catch (error: IllegalStateException) {
        PlaybackCommandResult.Failure(error.message)
    }

    private fun executeCommand(command: PlaybackCommand): PlaybackCommandResult =
        when (command) {
            is PlaybackCommand.Prepare -> prepare(command)
            PlaybackCommand.Play -> ifPrepared {
                player.start()
                eventListener?.invoke(PlaybackEvent.Playing)
            }
            PlaybackCommand.Pause -> ifPrepared {
                if (player.isPlaying) player.pause()
                eventListener?.invoke(PlaybackEvent.Paused)
            }
            PlaybackCommand.Stop -> {
                if (!released) player.reset()
                prepared = false
                eventListener?.invoke(PlaybackEvent.Stopped)
                PlaybackCommandResult.Executed
            }
            is PlaybackCommand.SeekTo -> ifPrepared {
                player.seekTo(command.positionMs)
                eventListener?.invoke(
                    PlaybackEvent.PositionChanged(command.positionMs, player.duration.coerceAtLeast(0))
                )
            }
            is PlaybackCommand.SetMuted -> ifNotReleased {
                val volume = if (command.muted) 0f else 1f
                player.setVolume(volume, volume)
                eventListener?.invoke(PlaybackEvent.MutedChanged(command.muted))
            }
            is PlaybackCommand.SetSpeed -> ifPrepared {
                player.playbackParams = player.playbackParams.setSpeed(command.speed.value)
                eventListener?.invoke(PlaybackEvent.SpeedChanged(command.speed))
            }
            PlaybackCommand.Release -> release()
        }

    override fun snapshot(): PlaybackSnapshot? = if (!prepared || released) {
        null
    } else {
        try {
            PlaybackSnapshot(player.currentPosition.coerceAtLeast(0), player.duration.coerceAtLeast(0))
        } catch (_: IllegalStateException) {
            null
        }
    }

    private fun prepare(command: PlaybackCommand.Prepare): PlaybackCommandResult = ifNotReleased {
        player.reset()
        prepared = false
        restoredPositionMs = command.restoredPositionMs
        player.setDataSource(context, command.source.toAndroidUri())
        player.prepareAsync()
    }

    private inline fun ifPrepared(block: () -> Unit): PlaybackCommandResult =
        if (!prepared || released) {
            PlaybackCommandResult.Failure("not_prepared")
        } else {
            block()
            PlaybackCommandResult.Executed
        }

    private inline fun ifNotReleased(block: () -> Unit): PlaybackCommandResult =
        if (released) {
            PlaybackCommandResult.Failure("released")
        } else {
            block()
            PlaybackCommandResult.Executed
        }

    private fun release(): PlaybackCommandResult {
        if (!released) {
            try {
                if (prepared) player.stop()
            } catch (_: IllegalStateException) {
                // Release still has to run when the native player is between states.
            }
            player.release()
            prepared = false
            released = true
            eventListener?.invoke(PlaybackEvent.Released)
            eventListener = null
        }
        return PlaybackCommandResult.Executed
    }

    private fun PlaybackSource.toAndroidUri(): Uri = when (this) {
        is PlaybackSource.Local -> Uri.fromFile(path.toFile())
        is PlaybackSource.Remote -> Uri.parse(location)
    }
}
