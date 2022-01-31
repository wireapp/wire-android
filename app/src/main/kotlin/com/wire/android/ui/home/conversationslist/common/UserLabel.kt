package com.wire.android.ui.main.conversationlist.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wire.android.ui.common.LegalHoldIndicator
import com.wire.android.ui.common.MembershipQualifierLabel
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.wireTypography

@Composable
fun UserLabel(userInfoLabel: UserInfoLabel, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        with(userInfoLabel) {
            ConversationName(labelName)

            if (membership != Membership.None) {
                Spacer(modifier = Modifier.width(6.dp))
                MembershipQualifierLabel(membership)
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
        style = MaterialTheme.wireTypography.body02
    )
}

data class UserInfoLabel(
    val labelName: String,
    val isLegalHold: Boolean,
    val membership: Membership,
)
