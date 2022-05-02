package com.wire.android.ui.home.conversationslist.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.ui.common.LegalHoldIndicator
import com.wire.android.ui.theme.wireTypography

@Composable
fun ConversationTitle(
    name: String,
    isLegalHold: Boolean = false,
    modifier: Modifier = Modifier,
    badges: @Composable () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Text(
            text = name,
            style = MaterialTheme.wireTypography.body02
        )
        badges()
        if (isLegalHold) {
            Spacer(modifier = Modifier.width(6.dp))
            LegalHoldIndicator()
        }
    }
}

@Preview
@Composable
fun ConversationTitlePreview() {
    ConversationTitle("name", true)
}
