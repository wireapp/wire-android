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

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.wirePrimaryButtonColors
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireDimensions

@Composable
fun HangUpButton(
    modifier: Modifier = Modifier.size(dimensions().defaultCallingHangUpButtonSize),
    onHangUpButtonClicked: () -> Unit
) {
    WirePrimaryButton(
        shape = CircleShape,
        modifier = modifier,
        colors = wirePrimaryButtonColors().copy(enabled = colorsScheme().callingHangupButtonColor),
        onClick = onHangUpButtonClicked,
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_call_reject),
                contentDescription = stringResource(id = R.string.content_description_calling_hang_up_call),
                tint = colorsScheme().onCallingHangupButtonColor
            )
        }
    )
}

@Preview
@Composable
fun PreviewComposableHangUpButton() {
    HangUpButton(modifier = Modifier
        .width(MaterialTheme.wireDimensions.outgoingCallHangUpButtonSize)
        .height(MaterialTheme.wireDimensions.outgoingCallHangUpButtonSize), onHangUpButtonClicked = { })
}
