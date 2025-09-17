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

package com.wire.android.ui.home.conversations.mention

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.model.Clickable
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.rowitem.RowItemTemplate
import com.wire.android.ui.common.UserBadge
import com.wire.android.ui.common.avatar.UserProfileAvatar
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.search.HighlightName
import com.wire.android.ui.home.conversations.search.HighlightSubtitle
import com.wire.android.ui.home.conversationslist.model.Membership

@Composable
fun MemberItemToMention(
    avatarData: UserAvatarData,
    name: String,
    label: String,
    membership: Membership,
    searchQuery: String,
    clickable: Clickable,
    modifier: Modifier = Modifier
) {
    RowItemTemplate(
        leadingIcon = {
            Row { UserProfileAvatar(avatarData) }
        },
        titleStartPadding = dimensions().spacing0x,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HighlightName(
                    name = name,
                    searchQuery = searchQuery,
                    modifier = Modifier.weight(weight = 1f, fill = false)
                )
                UserBadge(
                    membership = membership,
                    startPadding = dimensions().spacing8x
                )
            }
        },
        subtitle = {
            HighlightSubtitle(
                subTitle = label,
                searchQuery = searchQuery
            )
        },
        actions = { },
        clickable = clickable,
        modifier = modifier.padding(start = dimensions().spacing8x)
    )
}

@Preview
@Composable
fun PreviewMemberItemToMention() {
    MemberItemToMention(
        avatarData = UserAvatarData(),
        name = "name",
        label = "handle",
        membership = Membership.Federated,
        searchQuery = "na",
        clickable = Clickable(),
        modifier = Modifier
    )
}
