/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

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
    membership: Membership,
    connectionState: ConnectionState? = null,
    isDeleted: Boolean = false,
    startPadding: Dp = dimensions().spacing0x,
    topPadding: Dp = dimensions().spacing0x
) {
    if (connectionState == ConnectionState.BLOCKED) {
        Spacer(modifier = Modifier.width(startPadding))
        BlockedLabel()
    } else if (isDeleted) {
        Spacer(modifier = Modifier.width(startPadding))
        DeletedLabel()
    } else if (membership.hasLabel()) {
        Spacer(modifier = Modifier.width(startPadding))
        MembershipQualifierLabel(membership, Modifier.padding(top = topPadding))
    }
}
