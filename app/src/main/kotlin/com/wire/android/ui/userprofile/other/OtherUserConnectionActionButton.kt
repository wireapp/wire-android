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
import com.wire.android.R
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.kalium.logic.data.user.ConnectionState

@Composable
fun OtherUserConnectionActionButton(
    connectionStatus: ConnectionState,
    onSendConnectionRequest: () -> Unit,
    onOpenConversation: () -> Unit,
    onCancelConnectionRequest: () -> Unit,
    acceptConnectionRequest: () -> Unit,
    ignoreConnectionRequest: () -> Unit,
) {
    when (connectionStatus) {
        ConnectionState.SENT -> WireSecondaryButton(
            text = stringResource(R.string.connection_label_cancel_request),
            onClick = onCancelConnectionRequest
        )
        ConnectionState.ACCEPTED -> WirePrimaryButton(
            text = stringResource(R.string.label_open_conversation),
            onClick = onOpenConversation,
        )
        ConnectionState.PENDING -> Column {
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
            Spacer(modifier = Modifier.height(dimensions().spacing8x))
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
        ConnectionState.BLOCKED -> {
            WireSecondaryButton(
                text = stringResource(R.string.user_profile_unblock_user),
                onClick = { } //TODO
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
    OtherUserConnectionActionButton(
        connectionStatus = ConnectionState.ACCEPTED,
        onSendConnectionRequest = {},
        onOpenConversation = {},
        onCancelConnectionRequest = {},
        acceptConnectionRequest = {},
        ignoreConnectionRequest = {})
}
