package com.wire.android.ui.conversation.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wire.android.ui.conversation.all.model.ConversationInfo
import com.wire.android.ui.conversation.all.model.Membership

@Composable
fun UserLabel(conversationInfo: ConversationInfo, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        with(conversationInfo) {
            ConversationName(name)

            if (memberShip != Membership.None) {
                Spacer(modifier = Modifier.width(6.dp))
                MembershipQualifier(label = memberShip.label)
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
    Text(text = name, fontWeight = FontWeight.W500)
}

