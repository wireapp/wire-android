package com.wire.android.ui.calling.controlbuttons

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.button.wireSecondaryButtonColors
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions

@Composable
fun WireCallControlButton(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    icon: @Composable (iconColor: Color) -> Unit,
) {
    val iconColor = if (isSelected) colorsScheme().onCallingControlButtonActive else colorsScheme().onCallingControlButtonInactive
    WireSecondaryButton(
        modifier = modifier.size(dimensions().defaultCallingControlsSize),
        onClick = {},
        leadingIcon = { icon(iconColor) },
        shape = CircleShape,
        colors = with(colorsScheme()) {
            wireSecondaryButtonColors().copy(
                selected = callingControlButtonActive,
                selectedOutline = callingControlButtonActiveOutline,
                onSelected = onCallingControlButtonActive,
                enabled = callingControlButtonInactive,
                enabledOutline = callingControlButtonInactiveOutline,
                onEnabled = onCallingControlButtonInactive
            )
        },
        state = if (isSelected) WireButtonState.Selected else WireButtonState.Default
    )
}
