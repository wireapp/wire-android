package com.wire.android.ui.calling.controlButtons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.dimensions

@Composable
fun DeclineButton(buttonClicked: () -> Unit) {
    IconButton(
        modifier = Modifier
            .height(dimensions().initiatingCallHangUpButtonSize)
            .width(dimensions().initiatingCallHangUpButtonSize),
        onClick = buttonClicked
    ) {
        Image(
            painter = painterResource(
                id = R.drawable.ic_decline
            ),
            contentDescription = stringResource(id = R.string.calling_decline_call)
        )
    }
}

@Preview
@Composable
fun ComposableDeclineButtonPreview() {
    DeclineButton(buttonClicked = { })
}
