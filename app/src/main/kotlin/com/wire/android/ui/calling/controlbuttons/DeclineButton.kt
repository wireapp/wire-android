package com.wire.android.ui.calling.controlbuttons

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.dimensions

@Composable
fun DeclineButton(buttonClicked: () -> Unit) {
    IconButton(
        modifier = Modifier.size(dimensions().initiatingCallHangUpButtonSize),
        onClick = { }
    ) {
        Icon(
            modifier = Modifier
                .clickable(interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = false, radius = dimensions().initiatingCallHangUpButtonSize / 2),
                    role = Role.Button,
                    onClick = { buttonClicked() }),
            painter = painterResource(id = R.drawable.ic_decline),
            contentDescription = stringResource(id = R.string.content_description_calling_decline_call),
            tint = Color.Unspecified
        )
    }
}

@Preview
@Composable
fun ComposableDeclineButtonPreview() {
    DeclineButton(buttonClicked = { })
}
