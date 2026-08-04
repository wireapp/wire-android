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

import androidx.compose.runtime.Composable
import com.wire.android.audioplayer.AudioPlayer
import com.wire.android.navigation.WireNavigator
import com.wire.android.navigation.annotation.features.cells.WireCellsDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation

/**
 * Cells navigation entry point for the shared [AudioPlayer]. Reads the destination's [AudioPlayerNavArgs]
 * and delegates rendering + playback to the reusable player in `core:audio-player`.
 */
@WireCellsDestination(
    style = PopUpNavigationAnimation::class,
    navArgs = AudioPlayerNavArgs::class,
)
@Composable
fun CellAudioPlayerScreen(
    navigator: WireNavigator,
    navArgs: AudioPlayerNavArgs,
) {
    AudioPlayer(
        localPath = navArgs.localPath,
        contentUrl = navArgs.contentUrl,
        fileName = navArgs.fileName,
        onNavigateBack = navigator::navigateBack,
    )
}
