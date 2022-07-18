package com.wire.android.ui.userprofile.other

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.textfield.WirePrimaryButton

@Composable
fun OtherUserConnectionActionButton(
    connectionStatus: ConnectionStatus,
    onSendConnectionRequest: () -> Unit,
    onOpenConversation: () -> Unit,
    onCancelConnectionRequest: () -> Unit,
    acceptConnectionRequest: () -> Unit,
    ignoreConnectionRequest: () -> Unit,
) {
    when (connectionStatus) {
        is ConnectionStatus.Sent -> WireSecondaryButton(
            text = stringResource(R.string.connection_label_cancel_request),
            onClick = onCancelConnectionRequest
        )
        is ConnectionStatus.Connected -> WirePrimaryButton(
            text = stringResource(R.string.label_open_conversation),
            onClick = onOpenConversation,
        )
        is ConnectionStatus.Pending -> Column {
            WirePrimaryButton(
                text = stringResource(R.string.connection_label_accept),
                onClick = acceptConnectionRequest,
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_check_tick),
                        contentDescription = stringResource(R.string.content_description_right_arrow),
                        modifier = Modifier.padding(dimensions().spacing8x)
                    )
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            WirePrimaryButton(
                text = stringResource(R.string.connection_label_ignore),
                state = WireButtonState.Error,
                onClick = ignoreConnectionRequest,
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = stringResource(R.string.content_description_right_arrow),
                    )
                }
            )
        }
        else -> WirePrimaryButton(
            text = stringResource(R.string.connection_label_connect),
            onClick = onSendConnectionRequest,
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add_contact),
                    contentDescription = stringResource(R.string.content_description_right_arrow),
                    modifier = Modifier.padding(dimensions().spacing8x)
                )
            }
        )
    }
}

@Composable
@Preview
fun OtherUserConnectionActionButtonPreview() {
    OtherUserConnectionActionButton(ConnectionStatus.Connected, {}, {}, {}, {}, {})
}
