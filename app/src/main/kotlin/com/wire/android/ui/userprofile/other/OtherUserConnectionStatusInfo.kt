package com.wire.android.ui.userprofile.other

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.user.ConnectionState

@Composable
fun OtherUserConnectionStatusInfo(connectionStatus: ConnectionState, membership: Membership) {
    Box(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(start = dimensions().spacing32x, end = dimensions().spacing32x)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (connectionStatus == ConnectionState.PENDING)
                Text(
                    text = stringResource(R.string.connection_label_user_wants_to_conect),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.wireColorScheme.onSurface,
                    style = MaterialTheme.wireTypography.title02
                )
            Spacer(modifier = Modifier.height(24.dp))
            val descriptionResource = when (connectionStatus) {
                ConnectionState.PENDING, ConnectionState.IGNORED -> R.string.connection_label_accepting_request_description
                ConnectionState.ACCEPTED -> throw IllegalStateException("Unhandled Connected ConnectionStatus")
                else -> if (membership == Membership.None)
                    R.string.connection_label_member_not_conneted
                else R.string.connection_label_member_not_belongs_to_team
            }
            Text(
                text = stringResource(descriptionResource),
                textAlign = TextAlign.Center,
                color = MaterialTheme.wireColorScheme.labelText,
                style = MaterialTheme.wireTypography.body01
            )
        }
    }
}

@Composable
@Preview
fun OtherUserConnectionStatusInfoPreview() {
    OtherUserConnectionStatusInfo(ConnectionState.NOT_CONNECTED, Membership.Guest)
}
