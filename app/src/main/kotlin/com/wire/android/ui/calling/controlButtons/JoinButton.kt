package com.wire.android.ui.calling.controlButtons

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.ui.common.button.WireButton
import com.wire.android.ui.common.button.WireButtonState

@Composable
fun JoinButton(buttonClicked: () -> Unit) {
    WireButton(
        onClick = { },
        fillMaxWidth = false,
        shape = RoundedCornerShape(12.dp),
        text = "Join",
        state = WireButtonState.Positive
    )
}

@Preview
@Composable
fun JoinButtonPreview() {
    JoinButton {

    }
}
