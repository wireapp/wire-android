package com.wire.android.ui.home.conversationslist

import androidx.annotation.StringRes
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.ui.home.conversationslist.model.ConnectionInfo
import com.wire.android.ui.home.conversationslist.model.toMessageId
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.user.ConnectionState

@Composable
fun ConnectionLabel(connectionInfo: ConnectionInfo) {
    if (connectionInfo.connectionState == ConnectionState.PENDING) {
        Text(
            text = getConnectionStringLabel(labelId = connectionInfo.connectionState.toMessageId()),
            style = MaterialTheme.wireTypography.subline01.copy(
                color = MaterialTheme.wireColorScheme.secondaryText
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun getConnectionStringLabel(@StringRes labelId: Int) =
    if (labelId == -1) "" else stringResource(id = labelId)
