package com.wire.android.ui.home.conversationslist

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.home.conversationslist.common.GroupConversationAvatar
import com.wire.android.ui.home.conversationslist.common.ConversationTitle
import com.wire.android.ui.home.conversationslist.common.ConversationUserAvatar
import com.wire.android.ui.home.conversationslist.common.UserLabel
import com.wire.android.ui.home.conversationslist.model.ConversationType
import com.wire.android.ui.home.conversationslist.model.ConversationUnreadMention
import com.wire.android.ui.home.conversationslist.model.EventType
import com.wire.android.ui.home.conversationslist.model.MentionMessage
import com.wire.android.ui.home.conversationslist.model.toUserInfoLabel
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@Composable
fun MentionConversationItem(
    mention: ConversationUnreadMention,
    eventType: EventType? = null,
    onMentionItemClick: () -> Unit,
    onConversationItemLongClick: () -> Unit
) {
    when (val conversationType = mention.conversationType) {
        is ConversationType.GroupConversation -> {
            with(conversationType) {
                RowItemTemplate(
                    leadingIcon = { GroupConversationAvatar(colorValue = groupColorValue) },
                    title = { ConversationTitle(name = groupName, isLegalHold = conversationType.isLegalHold) },
                    subTitle = { MentionLabel(mentionMessage = mention.mentionInfo.mentionMessage) },
                    eventType = eventType,
                    onRowItemClicked = onMentionItemClick,
                    onRowItemLongClicked = onConversationItemLongClick
                )
            }
        }
        is ConversationType.PrivateConversation -> {
            with(conversationType) {
                RowItemTemplate(
                    leadingIcon = { with(conversationType.userInfo) { ConversationUserAvatar(avatarAsset, availabilityStatus) } },
                    title = { UserLabel(userInfoLabel = toUserInfoLabel()) },
                    subTitle = { MentionLabel(mentionMessage = mention.mentionInfo.mentionMessage) },
                    eventType = eventType,
                    onRowItemClicked = onMentionItemClick,
                    onRowItemLongClicked =  onConversationItemLongClick
                )
            }
        }
    }
}

@Composable
private fun MentionLabel(mentionMessage: MentionMessage) {
    Text(
        text = mentionMessage.toQuote(),
        style = MaterialTheme.wireTypography.subline01,
        color = MaterialTheme.wireColorScheme.secondaryText,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}


