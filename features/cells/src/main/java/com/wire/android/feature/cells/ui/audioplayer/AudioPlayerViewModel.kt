/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.feature.cells.ui.audioplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.mediaplayer.AndroidMediaPlayerPlaybackEngineFactory
import com.wire.media.player.MediaPlaybackCoordinator
import com.wire.media.player.PlaybackSource
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import okio.Path.Companion.toPath

class AudioPlayerViewModel @AssistedInject constructor(
    engineFactory: AndroidMediaPlayerPlaybackEngineFactory,
    @Assisted val localPath: String?,
    @Assisted val contentUrl: String?,
    @Assisted val fileName: String?,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(localPath: String?, contentUrl: String?, fileName: String?): AudioPlayerViewModel
    }

    private val coordinator = MediaPlaybackCoordinator(engineFactory.create(), viewModelScope)
    val state: StateFlow<AudioPlaybackState> = coordinator.state

    init {
        playbackSource()?.let(coordinator::prepare)
    }

    fun play() {
        coordinator.play()
    }

    fun pause() {
        coordinator.pause()
    }

    fun togglePlayPause() {
        coordinator.togglePlayPause()
    }

    fun seekTo(positionMs: Int) {
        coordinator.seekTo(positionMs)
    }

    private fun playbackSource(): PlaybackSource? = when {
        localPath != null -> PlaybackSource.Local(localPath.toPath())
        contentUrl != null -> PlaybackSource.Remote(contentUrl)
        else -> null
    }

    override fun onCleared() {
        coordinator.release()
        super.onCleared()
    }
}
