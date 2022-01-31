package com.wire.android.ui.home.conversations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.BackNavigationIconButton
import com.wire.android.ui.common.OnDropDownIconButton
import com.wire.android.ui.common.SurfaceBackgroundWrapper
import com.wire.android.ui.home.conversations.mock.mockMessages
import com.wire.android.ui.home.conversations.model.ConversationView
import com.wire.android.ui.home.conversations.model.Message
import com.wire.android.ui.theme.title01

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    conversationView: ConversationView,
    onBackButtonClick: () -> Unit
) {
    with(conversationView) {
        Scaffold(
            topBar = { ConversationScreenTopAppBar(name, onBackButtonClick, {}, {}, {}) },
            content = {
                ConversationScreenContent(messages = messages)
            })
    }
}

@Composable
private fun ConversationScreenContent(messages: List<Message>) {
    SurfaceBackgroundWrapper {
        LazyColumn {
            items(messages) { message ->
                MessageItem(message = message)
            }
        }
    }
}

@Composable
private fun ConversationScreenTopAppBar(
    title: String,
    onBackButtonClick: () -> Unit,
    onDropDownClick: () -> Unit,
    onSearchButtonClick: () -> Unit,
    onVideoButtonClick: () -> Unit
) {
    SmallTopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                //TODO:This Box is only for the design preview, this is going to be changed, ignore it during code-review
                Box(
                    modifier = Modifier
                        .background(color = Color.Green, shape = RoundedCornerShape(8.dp))
                        .width(24.dp)
                        .height(24.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.title01,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                OnDropDownIconButton(onDropDownClick = onDropDownClick)
                Spacer(Modifier.width(6.dp))
            }

        },
        navigationIcon = { BackNavigationIconButton(onBackButtonClick = onBackButtonClick) },
        actions = {
            //TODO: replace this button once Michal has defined it
            OutlinedButton(
                onClick = onSearchButtonClick,
                shape = RoundedCornerShape(size = 12.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.defaultMinSize(minWidth = 40.dp, minHeight = 32.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search_icon),
                    contentDescription = "",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(Modifier.width(6.dp))
            //TODO: replace this button once Michal has defined it
            OutlinedButton(
                onClick = onVideoButtonClick,
                shape = RoundedCornerShape(size = 12.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.defaultMinSize(minWidth = 40.dp, minHeight = 32.dp),
                contentPadding = PaddingValues(0.dp),

                ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_video_icon),
                    contentDescription = "",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(Modifier.width(6.dp))
        }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

@Preview
@Composable
fun ConversationScreenPreview() {
    ConversationScreen(ConversationView(name = "Conversation title", messages = mockMessages)) {}
}

@Preview("Topbar with a very long conversation title")
@Composable
fun ConversationScreenTopAppBarLongTitlePreview() {
    ConversationScreenTopAppBar(
        "This is some very very very very very very very very very very long conversation title",
        {},
        {},
        {},
        {},
    )
}

@Preview("Topbar with a short  conversation title")
@Composable
fun ConversationScreenTopAppBarShortTitlePreview() {
    ConversationScreenTopAppBar(
        "Short title",
        {},
        {},
        {},
        {},
    )
}
