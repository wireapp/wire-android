package com.wire.android.ui.common.textfield

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.ui.common.Icon
import com.wire.android.ui.common.button.IconAlignment
import com.wire.android.ui.common.button.WireButton
import com.wire.android.ui.common.button.WireButtonColors
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.wirePrimaryButtonColors
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun WirePrimaryButton(
    onClick: () -> Unit,
    loading: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    leadingIconAlignment: IconAlignment = IconAlignment.Center,
    trailingIcon: @Composable (() -> Unit)? = null,
    trailingIconAlignment: IconAlignment = IconAlignment.Border,
    text: String? = null,
    textStyle: TextStyle = MaterialTheme.wireTypography.button03,
    state: WireButtonState = WireButtonState.Default,
    minHeight: Dp = MaterialTheme.wireDimensions.buttonMinHeight,
    minWidth: Dp = MaterialTheme.wireDimensions.buttonMinWidth,
    fillMaxWidth: Boolean = true,
    shape: Shape = RoundedCornerShape(MaterialTheme.wireDimensions.buttonCornerSize),
    colors: WireButtonColors = wirePrimaryButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    borderWidth: Dp = 0.dp,
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


@Preview(name = "Default WirePrimaryButton")
@Composable
fun WirePrimaryButtonPreview() {
    WirePrimaryButton(onClick = { }, text = "text")
}

@Preview(name = "Default WirePrimaryButton with icons")
@Composable
fun WirePrimaryButtonWithIconsPreview() {
    WirePrimaryButton(
        onClick = { },
        text = "text",
        leadingIcon = Icons.Filled.Check.Icon(modifier = Modifier.padding(end = 8.dp)),
        trailingIcon = Icons.Filled.ChevronRight.Icon()
    )
}

@Preview(name = "Default narrow WirePrimaryButton with icon")
@Composable
fun WirePrimaryButtonNarrowWithIconsPreview() {
    WirePrimaryButton(
        onClick = { },
        text = "text",
        leadingIcon = Icons.Filled.Notifications.Icon(modifier = Modifier.padding(end = 8.dp)),
        leadingIconAlignment = IconAlignment.Center,
        fillMaxWidth = false
    )
}

@Preview(name = "Default small WirePrimaryButton only icon")
@Composable
fun WirePrimaryButtonSmallOnlyIconsPreview() {
    WirePrimaryButton(
        onClick = { },
        leadingIcon = Icons.Filled.Search.Icon(),
        leadingIconAlignment = IconAlignment.Center,
        fillMaxWidth = false,
        minHeight = 32.dp,
        minWidth = 40.dp,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
    )
}

@Preview(name = "Default Loading WirePrimaryButton")
@Composable
fun WirePrimaryButtonLoadingPreview() {
    WirePrimaryButton(onClick = { }, loading = true, text = "text")
}

@Preview(name = "Disabled WirePrimaryButton")
@Composable
fun WirePrimaryButtonDisabledPreview() {
    WirePrimaryButton(onClick = { }, state = WireButtonState.Disabled, text = "text")
}

@Preview(name = "Selected WirePrimaryButton")
@Composable
fun WirePrimaryButtonSelectedPreview() {
    WirePrimaryButton(onClick = { }, state = WireButtonState.Selected, text = "text")
}

@Preview(name = "Error WirePrimaryButton")
@Composable
fun WirePrimaryButtonErrorPreview() {
    WirePrimaryButton(onClick = { }, state = WireButtonState.Error, text = "text")
}


@Preview(name = "Three WirePrimaryButton with different states")
@Composable
fun WirePrimaryButtonSelected3Preview() {
    Row {
        WirePrimaryButton(
            onClick = { },
            state = WireButtonState.Selected,
            text = "text",
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp, 0.dp, 0.dp, 16.dp)
        )
        WirePrimaryButton(
            onClick = { },
            state = WireButtonState.Default,
            text = "text",
            modifier = Modifier.weight(1f),
            shape = RectangleShape
        )
        WirePrimaryButton(
            onClick = { },
            state = WireButtonState.Disabled,
            text = "text",
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(0.dp, 16.dp, 16.dp, 0.dp)
        )
    }
}
