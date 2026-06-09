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
package com.wire.android.feature.cells.ui.videoviewer

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.ramcosta.composedestinations.generated.cells.destinations.CellVideoViewerScreenDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CellVideoViewerViewModel @Inject constructor(
    @ApplicationContext context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val navArgs: CellVideoViewerNavArgs = CellVideoViewerScreenDestination.argsFrom(savedStateHandle)

    val localPath: String? = navArgs.localPath
    val contentUrl: String? = navArgs.contentUrl
    val fileName: String? = navArgs.fileName

    // Held in the ViewModel so playback survives configuration changes (e.g. rotating to full screen)
    // without re-buffering the media.
    val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        videoUri()?.let {
            setMediaItem(MediaItem.fromUri(it))
            prepare()
        }
    }

    private fun videoUri(): Uri? = when {
        localPath != null -> Uri.fromFile(File(localPath))
        contentUrl != null -> Uri.parse(contentUrl)
        else -> null
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
    }
}

