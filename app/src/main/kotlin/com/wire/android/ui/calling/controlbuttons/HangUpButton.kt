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

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.model.ClickBlockParams
import com.wire.android.ui.common.button.IconAlignment
import com.wire.android.ui.common.button.WireButtonColors
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WirePrimaryIconButton
import com.wire.android.ui.common.button.wirePrimaryButtonColors
import com.wire.android.ui.common.button.wireSecondaryButtonColors
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun HangUpButton(
    onHangUpButtonClicked: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = dimensions().bigCallingControlsSize,
    iconSize: Dp = dimensions().bigCallingHangUpButtonIconSize,
) {
    WirePrimaryIconButton(
        iconResource = R.drawable.ic_call_reject,
        contentDescription = R.string.content_description_calling_hang_up_call,
        state = WireButtonState.Error,
        shape = CircleShape,
        minSize = DpSize(size, size),
        minClickableSize = DpSize(size, size),
        iconSize = iconSize,
        onButtonClicked = onHangUpButtonClicked,
        modifier = modifier,
    )
}

@Composable
fun EmojiButton(
    emoji: String,
    onButtonClicked: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    shape: Shape = CircleShape,
    minSize: DpSize = MaterialTheme.wireDimensions.buttonSmallMinSize,
    minClickableSize: DpSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
    iconSize: Dp = dimensions().defaultCallingControlsIconSize,
    state: WireButtonState = WireButtonState.Default,
    colors: WireButtonColors = wireSecondaryButtonColors(),
    clickBlockParams: ClickBlockParams = ClickBlockParams()
) {
    WirePrimaryButton(
        onClick = onButtonClicked,
        loading = loading,
        trailingIcon = {
            Text(
                emoji,
                modifier = Modifier.size(iconSize)
            )
        },
        shape = shape,
        minSize = minSize,
        minClickableSize = minClickableSize,
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
        trailingIconAlignment = IconAlignment.Center,
        state = state,
        colors = colors,
        clickBlockParams = clickBlockParams,
        fillMaxWidth = false,
        modifier = modifier
    )
}
@PreviewMultipleThemes
@Composable
fun PreviewComposableHangUpButton() = WireTheme {
    HangUpButton(
        modifier = Modifier
            .width(MaterialTheme.wireDimensions.bigCallingControlsSize)
            .height(MaterialTheme.wireDimensions.bigCallingControlsSize),
        onHangUpButtonClicked = { }
    )
}
