package com.wire.android.ui.common

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.conversationslist.model.hasLabel
import com.wire.kalium.logic.data.user.ConnectionState

@Composable
fun UserBadge(membership: Membership, connectionState: ConnectionState? = null, startPadding: Dp = 0.dp) {
    if (connectionState == ConnectionState.BLOCKED) {
        Spacer(modifier = Modifier.width(startPadding))
        BlockedLabel()
    } else if (membership.hasLabel()) {
        Spacer(modifier = Modifier.width(startPadding))
        MembershipQualifierLabel(membership, Modifier.padding(top = dimensions().spacing8x))
    }
}
