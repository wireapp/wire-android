package com.wire.android.ui.home.conversations.mention

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.model.Clickable
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.UserBadge
import com.wire.android.ui.common.UserProfileAvatar
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
    searchQuery: String = "",
    clickable: Clickable,
    modifier: Modifier = Modifier
) {
    RowItemTemplate(
        leadingIcon = {
            Row { UserProfileAvatar(avatarData) }
        },
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
        modifier = modifier
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
        searchQuery = "search",
        clickable = Clickable(),
        modifier = Modifier
    )
}
