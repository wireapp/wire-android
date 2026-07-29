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
package com.wire.android.ui.home.conversations.audioplayer

import androidx.compose.runtime.Composable
import com.wire.android.audioplayer.AudioPlayer
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireRootDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation

/**
 * App navigation entry point for the shared [AudioPlayer]. Lets chat (and any app screen) play a
 * downloaded audio file in-app instead of handing it off to an external application.
 */
@WireRootDestination(
    navArgs = AudioPlayerNavArgs::class,
    style = PopUpNavigationAnimation::class,
)
@Composable
fun AudioPlayerScreen(
    navigator: Navigator,
    navArgs: AudioPlayerNavArgs,
) {
    AudioPlayer(
        localPath = navArgs.localPath,
        contentUrl = navArgs.contentUrl,
        fileName = navArgs.fileName,
        onNavigateBack = navigator::navigateBack,
    )
}