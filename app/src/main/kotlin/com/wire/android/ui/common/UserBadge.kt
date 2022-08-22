package com.wire.android.ui.common

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.conversationslist.model.hasLabel
import com.wire.kalium.logic.data.user.ConnectionState

/**
 * Widget for badge that is displayed next to the UserName.
 * It may be some of the Users Membership (Guest, Federated, External, Service),
 * or "Blocked" for the user that was blocked by me.
 *
 * Only 1 Label is displayed, so if user is blocked - "Blocked" label is displayed,
 * no matter what Membership value the user might have
 * If user is not blocked, then Membership Label is displayed.
 *
 * @param membership - users Membership that is used for membership Label
 * @param connectionState - users ConnectionState to get if user blocked or not,
 * may be null if don't want to display if user is blocked or not
 * @param startPadding - Dp of labels left padding
 */
@Composable
fun UserBadge(
    membership: Membership, connectionState: ConnectionState? = null,
    startPadding: Dp = dimensions().spacing0x,
    topPadding: Dp = dimensions().spacing0x
) {
    if (connectionState == ConnectionState.BLOCKED) {
        Spacer(modifier = Modifier.width(startPadding))
        BlockedLabel()
    } else if (membership.hasLabel()) {
        Spacer(modifier = Modifier.width(startPadding))
        MembershipQualifierLabel(membership, Modifier.padding(top = topPadding))
    }
}
