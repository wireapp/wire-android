package com.wire.android.ui.main.conversationlist

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.main.conversationlist.common.ConversationItem
import com.wire.android.ui.main.conversationlist.common.GroupConversationAvatar
import com.wire.android.ui.main.conversationlist.common.GroupName
import com.wire.android.ui.main.conversationlist.common.RowItem
import com.wire.android.ui.main.conversationlist.common.UnreadMentionBadge
import com.wire.android.ui.main.conversationlist.common.UserLabel
import com.wire.android.ui.main.conversationlist.common.folderWithElements
import com.wire.android.ui.main.conversationlist.model.Conversation
import com.wire.android.ui.main.conversationlist.model.EventType
import com.wire.android.ui.main.conversationlist.model.Mention
import com.wire.android.ui.main.conversationlist.model.MentionMessage
import com.wire.android.ui.main.conversationlist.model.toUserInfoLabel
import com.wire.android.ui.theme.subline01
import com.wire.android.ui.theme.wireColorScheme


@Composable
fun MentionScreen(
    unreadMentions: List<Mention> = emptyList(),
    allMentions: List<Mention> = emptyList(),
    onMentionItemClick: () -> Unit
) {
    MentionContent(
        unreadMentions = unreadMentions,
        allMentions = allMentions,
        onMentionItemClick = onMentionItemClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MentionContent(
    unreadMentions: List<Mention>,
    allMentions: List<Mention>,
    onMentionItemClick: () -> Unit
) {
    LazyColumn {
        folderWithElements(
            header = { stringResource(id = R.string.mention_label_unread_mentions) },
            items = unreadMentions
        ) { unreadMention ->
            MentionItem(
               mention = unreadMention,
                eventType = EventType.UnreadMention
            )
        }

        folderWithElements(
            header = { stringResource(R.string.mention_label_all_mentions) },
            items = allMentions
        ) { mention ->
            MentionItem(
                mention = mention,
            )
        }
    }
}

//
//@Composable
//fun UnreadMentionRowItem(unreadMention: Mention, onMentionItemClick: () -> Unit) {
//    RowItem(onRowItemClick = onMentionItemClick) {
//        MentionLabel(
//            mention = unreadMention,
//            modifier = Modifier
//                .weight(1f)
//                .padding(end = 16.dp)
//        )
//        UnreadMentionBadge(
//            modifier = Modifier
//                .wrapContentWidth()
//                .padding(start = 4.dp)
//        )
//    }
//}
//
//@Composable
//fun AllMentionRowItem(mention: Mention, onMentionItemClick: () -> Unit) {
//    RowItem(onRowItemClick = onMentionItemClick) {
//        MentionLabel(
//            mention = mention,
//            modifier = Modifier.padding(end = 42.dp)
//        )
//    }
//}
//

@Composable
private fun MentionItem(mention: Mention, eventType: EventType? = null) {
    when (val conversation = mention.conversation) {
        is Conversation.GroupConversation -> {
            with(conversation) {
                ConversationItem(
                    avatar = { GroupConversationAvatar(colorValue = groupColorValue) },
                    title = { GroupName(groupName) },
                    subTitle = { MentionLabel(mention.mentionInfo.mentionMessage) },
                    eventType = eventType
                )
            }
        }
        is Conversation.PrivateConversation -> {
            with(conversation) {
                ConversationItem(
                    avatar = { UserProfileAvatar() },
                    title = { UserLabel(toUserInfoLabel()) },
                    subTitle = { MentionLabel(mention.mentionInfo.mentionMessage) },
                    eventType = eventType
                )
            }
        }
    }
}

@Composable
fun MentionLabel(mentionMessage: MentionMessage) {
    Text(
        text = mentionMessage.toQuote(),
        style = MaterialTheme.typography.subline01,
        color = MaterialTheme.wireColorScheme.secondaryText,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}


