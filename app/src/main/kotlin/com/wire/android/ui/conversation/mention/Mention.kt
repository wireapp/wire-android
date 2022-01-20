package com.wire.android.ui.conversation.mention

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.conversation.common.FolderHeader
import com.wire.android.ui.conversation.common.UnreadMentionBadge
import com.wire.android.ui.conversation.common.UserInfoLabel
import com.wire.android.ui.conversation.common.WhiteBackgroundWrapper
import com.wire.android.ui.conversation.mention.model.Mention
import com.wire.android.ui.theme.subLine1

@Preview
@Composable
fun Mention(viewModel: MentionViewModel = MentionViewModel()) {
    val uiState by viewModel.state.collectAsState()

    MentionScreen(uiState = uiState)
}

@Composable
private fun MentionScreen(uiState: MentionState) {
    Scaffold(
        content = { MentionContent(uiState) }
    )
}

@Composable
private fun MentionContent(uiState: MentionState) {
    with(uiState) {
        LazyColumn {
            if (unreadMentions.isNotEmpty()) {
                item { FolderHeader(name = stringResource(R.string.mention_label_unread_mentions)) }
                items(unreadMentions) { unreadMention ->
                    UnreadMentionRowItem(unreadMention)
                }
            }

            if (allMentions.isNotEmpty()) {
                item { FolderHeader(name = stringResource(R.string.mention_label_all_mentions)) }
                items(allMentions) { mention ->
                    AllMentionRowItem(mention)
                }
            }
        }
    }
}

@Composable
fun UnreadMentionRowItem(unreadMention: Mention) {
    WhiteBackgroundWrapper(
        content = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
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
        },
        modifier = Modifier.padding(0.5.dp)
    )
}

@Composable
fun AllMentionRowItem(mention: Mention) {
    WhiteBackgroundWrapper(
        content = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                MentionLabel(
                    mention = mention,
                    modifier = Modifier.padding(end = 42.dp)
                )
            }
        },
        modifier = Modifier.padding(0.5.dp)
    )
}

@Composable
fun MentionLabel(mention: Mention, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        with(mention) {
            UserInfoLabel(conversationInfo = conversation.conversationInfo)
            Text(
                text = mentionInfo.mentionMessage.toQuote(),
                style = MaterialTheme.typography.subLine1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}


