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
import androidx.compose.material.ripple
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
fun SpeakerButton(
    modifier: Modifier = Modifier.size(dimensions().defaultCallingControlsSize),
    isSpeakerOn: Boolean,
    onSpeakerButtonClicked: () -> Unit
) {
    WireCallControlButton(
        isSelected = isSpeakerOn,
        modifier = modifier
    ) { iconColor ->
        Icon(
            modifier = modifier
                .wrapContentSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = dimensions().defaultCallingControlsSize / 2),
                    role = Role.Button,
                    onClick = { onSpeakerButtonClicked() }
                ),
            painter = painterResource(
                id = if (isSpeakerOn)
                    R.drawable.ic_speaker_on
                else R.drawable.ic_speaker_off
            ),
            contentDescription = stringResource(
                id = if (isSpeakerOn) R.string.content_description_calling_turn_speaker_off
                else R.string.content_description_calling_turn_speaker_on
            ),
            tint = iconColor
        )
    }
}

@Preview
@Composable
fun PreviewSpeakerButton() {
    SpeakerButton(isSpeakerOn = true, onSpeakerButtonClicked = { })
}
