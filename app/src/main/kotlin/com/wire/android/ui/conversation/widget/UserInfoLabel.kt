package com.wire.android.ui.conversation.widget

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.ui.conversation.all.model.ConversationInfo
import com.wire.android.ui.conversation.all.model.Membership

@Composable
fun UserInfoLabel(conversationInfo: ConversationInfo, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        with(conversationInfo) {
            ConversationName(name)

            if (memberShip != Membership.None) {
                Spacer(modifier = Modifier.width(6.dp))
                MembershipQualifier(label = stringResource(id = memberShip.stringResourceId))
            }

            if (isLegalHold) {
                Spacer(modifier = Modifier.width(6.dp))
                LegalHoldIndicator()
            }
        }
    }
}

@Composable
private fun ConversationName(name: String) {
    Text(
        text = name,
        style = MaterialTheme.typography.body2
    )
}

