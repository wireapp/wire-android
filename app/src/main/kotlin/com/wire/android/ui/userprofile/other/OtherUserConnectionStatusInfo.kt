/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 *
 */

package com.wire.android.ui.userprofile.other

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.wire.android.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
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
            if (connectionStatus == ConnectionState.PENDING) {
                Text(
                    text = stringResource(R.string.connection_label_user_wants_to_conect),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.wireColorScheme.onSurface,
                    style = MaterialTheme.wireTypography.title02
                )
            }

            descriptionResourceForConnectionAndMembership(connectionStatus, membership)?.let {
                Text(
                    text = stringResource(it),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.wireColorScheme.labelText,
                    style = MaterialTheme.wireTypography.body01
                )
                VerticalSpace.x24()
            }
        }
    }
}

@Composable
private fun descriptionResourceForConnectionAndMembership(
    connectionStatus: ConnectionState,
    membership: Membership
) = when (connectionStatus) {
    ConnectionState.PENDING, ConnectionState.IGNORED -> R.string.connection_label_accepting_request_description
    ConnectionState.ACCEPTED, ConnectionState.BLOCKED -> null
    else -> if (membership == Membership.None) {
        R.string.connection_label_member_not_conneted
    } else {
        R.string.connection_label_member_not_belongs_to_team
    }
}

@Composable
@Preview
fun PreviewOtherUserConnectionStatusInfo() {
    OtherUserConnectionStatusInfo(ConnectionState.NOT_CONNECTED, Membership.Guest)
}
