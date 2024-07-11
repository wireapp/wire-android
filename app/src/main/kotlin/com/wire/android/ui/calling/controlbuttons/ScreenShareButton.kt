/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.ui.calling.controlbuttons

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.wire.android.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun ScreenShareButton(
    isScreenShareOn: Boolean,
    onScreenShareButtonClicked: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = dimensions().defaultCallingControlsSize
) {
    WireCallControlButton(
        isSelected = isScreenShareOn,
        iconResId = R.drawable.ic_share,
        contentDescription = R.string.content_description_calling_turn_speaker_on,
        onClick = onScreenShareButtonClicked,
        size = size,
        modifier = modifier
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewScreenShareButtonOn() = WireTheme {
    ScreenShareButton(isScreenShareOn = true, onScreenShareButtonClicked = { })
}

@PreviewMultipleThemes
@Composable
fun PreviewScreenShareButtonOff() = WireTheme {
    ScreenShareButton(isScreenShareOn = false, onScreenShareButtonClicked = { })
}
