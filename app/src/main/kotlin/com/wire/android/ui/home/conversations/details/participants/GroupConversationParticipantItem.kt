package com.wire.android.ui.home.conversations.details.participants

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.MembershipQualifierLabel
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.newconversation.search.HighlightName
import com.wire.android.ui.home.newconversation.search.HighlightSubtitle
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.data.user.UserId

@Composable
fun GroupConversationParticipantItem(
    uiParticipant: UIParticipant,
    searchQuery: String = String.EMPTY,
    onRowItemClicked: () -> Unit = {},
    onRowItemLongClicked: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    RowItemTemplate(
        leadingIcon = { UserProfileAvatar(uiParticipant.avatarData) },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HighlightName(
                    name = uiParticipant.name,
                    searchQuery = searchQuery,
                    modifier = Modifier.weight(weight = 1f, fill = false)
                )
                if (uiParticipant.membership != Membership.None) {
                    Spacer(modifier = Modifier.width(6.dp))
                    MembershipQualifierLabel(uiParticipant.membership)
                }
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
        onRowItemClicked = onRowItemClicked,
        onRowItemLongClicked = onRowItemLongClicked,
        modifier = modifier
    )
}

@Preview
@Composable
fun GroupConversationParticipantItemPreview() {
    GroupConversationParticipantItem(UIParticipant(UserId("0", ""), "name", "handle", false, UserAvatarData(), Membership.Guest))
}
