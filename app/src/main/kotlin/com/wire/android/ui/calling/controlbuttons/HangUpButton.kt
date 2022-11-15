package com.wire.android.ui.calling.controlbuttons

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import com.wire.android.R
import com.wire.android.ui.theme.wireDimensions

@Composable
fun HangUpButton(
    modifier: Modifier, onHangUpButtonClicked: () -> Unit
) {
    var size by remember { mutableStateOf(IntSize.Zero) }

    IconButton(modifier = modifier.onSizeChanged {
        size = it
    }, onClick = { }) {
        var rippleSize = with(LocalDensity.current) {
            size.width.toDp() / 2
        }
        Icon(
            modifier = modifier
                .clickable(interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = false, radius = rippleSize),
                    role = Role.Button,
                    onClick = {
                        onHangUpButtonClicked()
                    }),
            painter = painterResource(id = R.drawable.ic_hang_up),
            contentDescription = stringResource(id = R.string.content_description_calling_hang_up_call),
            tint = Color.Unspecified
        )
    }
}

@Preview
@Composable
fun ComposableHangUpButtonPreview() {
    HangUpButton(modifier = Modifier
        .width(MaterialTheme.wireDimensions.initiatingCallHangUpButtonSize)
        .height(MaterialTheme.wireDimensions.initiatingCallHangUpButtonSize), onHangUpButtonClicked = { })
}
