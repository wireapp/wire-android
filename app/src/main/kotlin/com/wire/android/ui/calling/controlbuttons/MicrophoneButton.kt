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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.dimensions

@Composable
fun MicrophoneButton(
    modifier: Modifier = Modifier.size(dimensions().defaultCallingControlsSize),
    isMuted: Boolean,
    onMicrophoneButtonClicked: () -> Unit
) {
    WireCallControlButton(
        isSelected = !isMuted,
        modifier = modifier
    ) { iconColor ->
        Icon(
            modifier = modifier
                .wrapContentSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = false, radius = dimensions().defaultCallingControlsSize / 2),
                    role = Role.Button,
                    onClick = { onMicrophoneButtonClicked() }
                ),
            painter = painterResource(
                id = if (isMuted) {
                    R.drawable.ic_microphone_off
                } else {
                    R.drawable.ic_microphone_on
                }
            ),
            contentDescription = stringResource(
                id = if (isMuted) R.string.content_description_calling_unmute_call
                else R.string.content_description_calling_mute_call
            ),
            tint = iconColor
        )
    }
}

@Preview
@Composable
fun PreviewComposableMicrophoneButton() {
    MicrophoneButton(isMuted = true, onMicrophoneButtonClicked = { })
}
