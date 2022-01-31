package com.wire.android.ui.main.conversationlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.wire.android.ui.home.conversationslist.common.RowItem
import com.wire.android.ui.home.conversationslist.common.UnreadMentionBadge
import com.wire.android.ui.main.conversationlist.common.UserLabel
import com.wire.android.ui.home.conversationslist.common.folderWithElements
import com.wire.android.ui.home.conversationslist.model.Mention
import com.wire.android.ui.home.conversationslist.model.toUserInfoLabel
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
            UnreadMentionRowItem(
                unreadMention = unreadMention,
                onMentionItemClick = onMentionItemClick
            )
        }

        folderWithElements(
            header = { stringResource(R.string.mention_label_all_mentions) },
            items = allMentions
        ) { mention ->
            AllMentionRowItem(
                mention = mention,
                onMentionItemClick = onMentionItemClick
            )
        }
    }
}

@Composable
fun UnreadMentionRowItem(unreadMention: Mention, onMentionItemClick: () -> Unit) {
    RowItem(onRowItemClick = onMentionItemClick) {
        MentionLabel(
            mention = unreadMention,
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        )
        UnreadMentionBadge(
            modifier = Modifier
                .wrapContentWidth()
                .padding(start = 4.dp)
        )
    }
}

@Composable
fun AllMentionRowItem(mention: Mention, onMentionItemClick: () -> Unit) {
    RowItem(onRowItemClick = onMentionItemClick) {
        MentionLabel(
            mention = mention,
            modifier = Modifier.padding(end = 42.dp)
        )
    }
}

@Composable
fun MentionLabel(mention: Mention, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        with(mention) {
            UserProfileAvatar(avatarUrl = conversation.userInfo.avatarUrl, onClick = {})
            Column {
                UserLabel(conversation.toUserInfoLabel())
                Text(
                    text = mentionInfo.mentionMessage.toQuote(),
                    style = MaterialTheme.typography.subline01,
                    color = MaterialTheme.wireColorScheme.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}


