package com.wire.android.ui.home.conversations.details.participants

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.UserBadge
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.ui.home.conversations.search.HighlightName
import com.wire.android.ui.home.conversations.search.HighlightSubtitle
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.data.user.UserId

@Composable
fun GroupConversationParticipantItem(
    uiParticipant: UIParticipant,
    searchQuery: String = String.EMPTY,
    clickable: Clickable,
    modifier: Modifier = Modifier
) {
    RowItemTemplate(
        leadingIcon = { UserProfileAvatar(uiParticipant.avatarData) },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HighlightName(
                    name = if (uiParticipant.unavailable) stringResource(R.string.username_unavailable_label) else uiParticipant.name,
                    searchQuery = searchQuery,
                    modifier = Modifier.weight(weight = 1f, fill = false)
                )
                UserBadge(
                    membership = uiParticipant.membership,
                    connectionState = uiParticipant.connectionState,
                    startPadding = dimensions().spacing6x
                )
            }

        },
        subtitle = { HighlightSubtitle(subTitle = uiParticipant.handle, searchQuery = searchQuery) },
        actions = {
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(end = MaterialTheme.wireDimensions.spacing8x)
            ) {
                ArrowRightIcon(Modifier.align(Alignment.TopEnd))
            }
        },
        clickable = clickable,
        modifier = modifier
    )
}

@Preview
@Composable
fun GroupConversationParticipantItemPreview() {
    GroupConversationParticipantItem(
        UIParticipant(UserId("0", ""), "name", "handle", false, UserAvatarData(), Membership.Guest),
        clickable = Clickable(enabled = true) {}
    )
}
