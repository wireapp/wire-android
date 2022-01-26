package com.wire.android.ui.home.conversations.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.ui.home.conversations.all.model.Membership
import com.wire.android.ui.theme.body02

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
                MembershipQualifier(label = stringResource(id = membership.stringResourceId))
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
        style = MaterialTheme.typography.body02
    )
}

data class UserInfoLabel(
    val labelName: String,
    val isLegalHold: Boolean,
    val membership: Membership,
)
