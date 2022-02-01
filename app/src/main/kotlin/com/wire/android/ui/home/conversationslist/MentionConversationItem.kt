package com.wire.android.ui.home.conversationslist

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.home.conversations.common.ConversationItemTemplate
import com.wire.android.ui.home.conversations.common.GroupConversationAvatar
import com.wire.android.ui.home.conversations.common.GroupName
import com.wire.android.ui.home.conversationslist.model.ConversationType
import com.wire.android.ui.home.conversationslist.model.EventType
import com.wire.android.ui.home.conversationslist.model.Mention
import com.wire.android.ui.home.conversationslist.model.MentionMessage
import com.wire.android.ui.home.conversationslist.model.toUserInfoLabel
import com.wire.android.ui.main.conversationlist.common.UserLabel
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography


@Composable
fun MentionConversationItem(
    mention: Mention,
    eventType: EventType? = null,
    onMentionItemClick: () -> Unit
) {
    when (val conversationType = mention.conversationType) {
        is ConversationType.GroupConversation -> {
            with(conversationType) {
                ConversationItemTemplate(
                    leadingIcon = { GroupConversationAvatar(colorValue = groupColorValue) },
                    title = { GroupName(name = groupName) },
                    subTitle = { MentionLabel(mentionMessage = mention.mentionInfo.mentionMessage) },
                    eventType = eventType,
                    onConversationItemClick = onMentionItemClick
                )
            }
        }
        is ConversationType.PrivateConversation -> {
            with(conversationType) {
                ConversationItemTemplate(
                    leadingIcon = { UserProfileAvatar() },
                    title = { UserLabel(userInfoLabel = toUserInfoLabel()) },
                    subTitle = { MentionLabel(mentionMessage = mention.mentionInfo.mentionMessage) },
                    eventType = eventType,
                    onConversationItemClick = onMentionItemClick
                )
            }
        }
    }
}

@Composable
fun MentionLabel(mentionMessage: MentionMessage) {
    Text(
        text = mentionMessage.toQuote(),
        style = MaterialTheme.wireTypography.subline01,
        color = MaterialTheme.wireColorScheme.secondaryText,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}


