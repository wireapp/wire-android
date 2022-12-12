package com.wire.android.ui.home.messagecomposer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Transition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryIconButton
import com.wire.android.ui.common.button.wireSendPrimaryButtonColors
import com.wire.android.ui.common.dimensions

@ExperimentalAnimationApi
@Composable
fun SendActions(
    modifier: Modifier,
    messageComposerState: MessageComposerInnerState,
    transition: Transition<MessageComposeInputState>,
    onSendButtonClicked: () -> Unit
) {
    Box(modifier) {
        Row(Modifier.padding(end = dimensions().spacing8x).background(Color.Black)) {
//            if (messageComposerState.sendButtonEnabled) {
//                ScheduleMessageButton()
//            }
            transition.AnimatedVisibility(
                visible = { messageComposerState.messageComposeInputState != MessageComposeInputState.Enabled },
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                SendButton(
                    isEnabled = messageComposerState.sendButtonEnabled,
                    onSendButtonClicked = onSendButtonClicked
                )
            }
        }
    }
}

@Composable
private fun ScheduleMessageButton() {
    IconButton(onClick = { }) {
        Icon(
            painter = painterResource(id = R.drawable.ic_timer),
            contentDescription = stringResource(R.string.content_description_timed_message_button),
        )
    }
}

@Composable
private fun SendButton(
    isEnabled: Boolean,
    onSendButtonClicked: () -> Unit
) {
    WirePrimaryIconButton(
        onButtonClicked = onSendButtonClicked,
        iconResource = R.drawable.ic_send,
        contentDescription = R.string.content_description_send_button,
        state = if (isEnabled) WireButtonState.Default else WireButtonState.Disabled,
        shape = RoundedCornerShape(dimensions().spacing20x),
        colors = wireSendPrimaryButtonColors(),
        blockUntilSynced = true,
        minHeight = dimensions().spacing40x,
        minWidth = dimensions().spacing40x
    )
}
