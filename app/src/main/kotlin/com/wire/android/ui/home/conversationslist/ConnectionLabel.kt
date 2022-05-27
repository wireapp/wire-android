package com.wire.android.ui.home.conversationslist

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.ui.home.conversationslist.model.ConnectionInfo
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@Composable
fun ConnectionLabel(connectionInfo: ConnectionInfo) {
    androidx.compose.material.Text(
        text = connectionInfo.message, style = MaterialTheme.wireTypography.subline01.copy(
            color = MaterialTheme.wireColorScheme.secondaryText
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

