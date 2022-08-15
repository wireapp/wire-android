package com.wire.android.ui.home.conversationslist.common

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.MembershipQualifierLabel
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.conversationslist.model.hasLabel

@Composable
fun UserLabel(userInfoLabel: UserInfoLabel, modifier: Modifier = Modifier) {
    with(userInfoLabel) {
        ConversationTitle(
            name = if (unavailable) stringResource(id = R.string.unavailable_user_label) else labelName,
            isLegalHold = isLegalHold,
            modifier = modifier,
            badges = {
                if (membership.hasLabel()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    MembershipQualifierLabel(membership)
                }
            }
        )
    }
}

data class UserInfoLabel(
    val labelName: String,
    val isLegalHold: Boolean,
    val membership: Membership,
    val unavailable: Boolean = false
)
