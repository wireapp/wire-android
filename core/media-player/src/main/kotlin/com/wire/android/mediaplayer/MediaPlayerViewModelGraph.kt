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
@file:Suppress("MatchingDeclarationName")

package com.wire.android.mediaplayer

import androidx.compose.runtime.Composable
import com.wire.android.di.metro.sessionKeyedAssistedMetroViewModel
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory

interface MediaPlayerManualViewModelFactory : ManualViewModelAssistedFactory {
    fun videoPlayerViewModel(
        localPath: String?,
        contentUrl: String?,
        fileName: String?,
    ): VideoPlayerViewModel
}

@Composable
fun videoPlayerViewModel(
    localPath: String?,
    contentUrl: String?,
    fileName: String?,
): VideoPlayerViewModel =
    sessionKeyedAssistedMetroViewModel<VideoPlayerViewModel, MediaPlayerManualViewModelFactory>(
        key = "video_player_${localPath ?: contentUrl}"
    ) {
        videoPlayerViewModel(localPath, contentUrl, fileName)
    }
