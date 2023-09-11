/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.common.button

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.wire.android.model.ClickBlockParams
import com.wire.android.ui.common.Icon
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun WireTertiaryButton(
    onClick: () -> Unit,
    loading: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    leadingIconAlignment: IconAlignment = IconAlignment.Center,
    trailingIcon: @Composable (() -> Unit)? = null,
    trailingIconAlignment: IconAlignment = IconAlignment.Border,
    text: String? = null,
    fillMaxWidth: Boolean = true,
    textStyle: TextStyle = MaterialTheme.wireTypography.button04,
    state: WireButtonState = WireButtonState.Default,
    clickBlockParams: ClickBlockParams = ClickBlockParams(),
    minSize: DpSize = MaterialTheme.wireDimensions.buttonMinSize,
    minClickableSize: DpSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
    shape: Shape = RoundedCornerShape(MaterialTheme.wireDimensions.buttonCornerSize),
    colors: WireButtonColors = wireTertiaryButtonColors(),
    elevation: ButtonElevation? = null,
    borderWidth: Dp = 1.dp,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = MaterialTheme.wireDimensions.buttonHorizontalContentPadding,
        vertical = MaterialTheme.wireDimensions.buttonVerticalContentPadding
    ),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    modifier: Modifier = Modifier,
) = WireButton(
    onClick = onClick,
    loading = loading,
    leadingIcon = leadingIcon,
    leadingIconAlignment = leadingIconAlignment,
    trailingIcon = trailingIcon,
    trailingIconAlignment = trailingIconAlignment,
    text = text,
    textStyle = textStyle,
    state = state,
    clickBlockParams = clickBlockParams,
    minSize = minSize,
    minClickableSize = minClickableSize,
    fillMaxWidth = fillMaxWidth,
    shape = shape,
    colors = colors,
    elevation = elevation,
    borderWidth = borderWidth,
    contentPadding = contentPadding,
    interactionSource = interactionSource,
    modifier = modifier
)

@Preview(name = "Default WireSecondaryButton")
@Composable
fun PreviewWireTertiaryButton() {
    WireTertiaryButton(onClick = { }, text = "text")
}

@Preview(name = "Default narrow WireTertiaryButton with icon")
@Composable
fun PreviewWireTertiaryButtonNarrowWithIcons() {
    WireTertiaryButton(
        onClick = { },
        text = "text",
        leadingIcon = Icons.Filled.Notifications.Icon(modifier = Modifier.padding(end = 8.dp)),
        leadingIconAlignment = IconAlignment.Center,
        fillMaxWidth = false
    )
}

@Preview(name = "Default narrow WireTertiaryButton only icon")
@Composable
fun PreviewWireTertiaryButtonNarrowOnlyIcons() {
    WireTertiaryButton(
        onClick = { },
        leadingIcon = Icons.Filled.Search.Icon(),
        leadingIconAlignment = IconAlignment.Center,
        fillMaxWidth = false
    )
}

@Preview(name = "Default narrow Disabled WireSecondaryButton")
@Composable
fun PreviewWireTertiaryButtonDisabled() {
    WireTertiaryButton(onClick = { }, state = WireButtonState.Disabled, text = "text", fillMaxWidth = false)
}

@Preview(name = "Selected narrow WireSecondaryButton")
@Composable
fun PreviewWireTertiaryButtonSelected() {
    WireTertiaryButton(onClick = { }, state = WireButtonState.Selected, text = "text", fillMaxWidth = false)
}

@Preview(name = "Error narrow WireSecondaryButton")
@Composable
fun PreviewWireTertiaryButtonError() {
    WireTertiaryButton(onClick = { }, state = WireButtonState.Error, text = "text", fillMaxWidth = false)
}
