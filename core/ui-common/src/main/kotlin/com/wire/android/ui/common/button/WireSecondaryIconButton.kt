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

package com.wire.android.ui.common.button

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.wire.android.model.ClickBlockParams
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireDimensions

@Composable
fun WireSecondaryIconButton(
    onButtonClicked: () -> Unit,
    loading: Boolean = false,
    @DrawableRes iconResource: Int,
    @StringRes contentDescription: Int,
    shape: Shape = RoundedCornerShape(MaterialTheme.wireDimensions.buttonCornerSize),
    minSize: DpSize = MaterialTheme.wireDimensions.buttonSmallMinSize,
    minClickableSize: DpSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
    iconSize: Dp = dimensions().wireIconButtonSize,
    state: WireButtonState = WireButtonState.Default,
    colors: WireButtonColors = wireSecondaryButtonColors(),
    clickBlockParams: ClickBlockParams = ClickBlockParams(),
    modifier: Modifier = Modifier,
    fillMaxWidth: Boolean = false
) {
    WireSecondaryButton(
        onClick = onButtonClicked,
        loading = loading,
        trailingIcon = {
            Icon(
                painter = painterResource(id = iconResource),
                contentDescription = stringResource(contentDescription),
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
        fillMaxWidth = fillMaxWidth,
        modifier = modifier
    )
}

@Preview
@Composable
fun PreviewWireSecondaryIconButton() {
    WireSecondaryIconButton({}, false, com.google.android.material.R.drawable.m3_password_eye, 0)
}

@Preview
@Composable
fun PreviewWireSecondaryIconButtonLoading() {
    WireSecondaryIconButton({}, true, com.google.android.material.R.drawable.m3_password_eye, 0)
}

@Preview
@Composable
fun PreviewWireSecondaryIconButtonRound() {
    WireSecondaryIconButton(
        {},
        false,
        com.google.android.material.R.drawable.m3_password_eye,
        0,
        CircleShape,
        DpSize(40.dp, 40.dp),
        DpSize(48.dp, 48.dp)
    )
}
