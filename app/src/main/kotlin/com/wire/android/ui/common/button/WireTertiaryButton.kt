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
import androidx.compose.ui.unit.dp
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
    textStyle: TextStyle = MaterialTheme.wireTypography.button04,
    state: WireButtonState = WireButtonState.Default,
    minHeight: Dp = MaterialTheme.wireDimensions.buttonMinHeight,
    minWidth: Dp = MaterialTheme.wireDimensions.buttonMinWidth,
    fillMaxWidth: Boolean = true,
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
    minHeight = minHeight,
    minWidth = minWidth,
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
private fun WireTertiaryButtonPreview() {
    WireTertiaryButton(onClick = { }, text = "text")
}

@Preview(name = "Default narrow WireTertiaryButton with icon")
@Composable
private fun WireTertiaryButtonNarrowWithIconsPreview() {
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
private fun WireTertiaryButtonNarrowOnlyIconsPreview() {
    WireTertiaryButton(
        onClick = { },
        leadingIcon = Icons.Filled.Search.Icon(),
        leadingIconAlignment = IconAlignment.Center,
        fillMaxWidth = false
    )
}

@Preview(name = "Default narrow Disabled WireSecondaryButton")
@Composable
private fun WireTertiaryButtonDisabledPreview() {
    WireTertiaryButton(onClick = { }, state = WireButtonState.Disabled, text = "text", fillMaxWidth = false)
}

@Preview(name = "Selected narrow WireSecondaryButton")
@Composable
private fun WireTertiaryButtonSelectedPreview() {
    WireTertiaryButton(onClick = { }, state = WireButtonState.Selected, text = "text", fillMaxWidth = false)
}

@Preview(name = "Error narrow WireSecondaryButton")
@Composable
private fun WireTertiaryButtonErrorPreview() {
    WireTertiaryButton(onClick = { }, state = WireButtonState.Error, text = "text", fillMaxWidth = false)
}

