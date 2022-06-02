package com.wire.android.ui.home.conversations

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.OnDropDownIconButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.topappbar.BackNavigationIconButton
import com.wire.android.ui.home.conversationslist.common.GroupConversationAvatar
import com.wire.android.ui.theme.wireTypography

@Composable
fun ConversationScreenTopAppBar(
    title: String,
    avatar: @Composable () -> Unit = {},
    onBackButtonClick: () -> Unit,
    onDropDownClick: () -> Unit,
    onSearchButtonClick: () -> Unit,
    onPhoneButtonClick: () -> Unit
) {
    SmallTopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                avatar()
                Spacer(Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.wireTypography.title01,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                OnDropDownIconButton(onDropDownClick = onDropDownClick)
            }
        },
        navigationIcon = { BackNavigationIconButton(onBackButtonClick = onBackButtonClick) },
        actions = {
            WireSecondaryButton(
                onClick = onSearchButtonClick,
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_search),
                        contentDescription = stringResource(R.string.content_description_conversation_search_icon),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                },
                fillMaxWidth = false,
                minHeight = 32.dp,
                minWidth = 40.dp,
                shape = RoundedCornerShape(size = 12.dp),
                contentPadding = PaddingValues(0.dp)
            )
            Spacer(Modifier.width(6.dp))
            WireSecondaryButton(
                onClick = onPhoneButtonClick,
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_phone),
                        contentDescription = stringResource(R.string.content_description_conversation_phone_icon),
                    )
                },
                fillMaxWidth = false,
                minHeight = 32.dp,
                minWidth = 40.dp,
                shape = RoundedCornerShape(size = 12.dp),
                contentPadding = PaddingValues(0.dp)
            )
            Spacer(Modifier.width(6.dp))
        }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

@Preview("Topbar with a very long conversation title")
@Composable
fun ConversationScreenTopAppBarLongTitlePreview() {
    ConversationScreenTopAppBar(
        "This is some very very very very very very very very very very long conversation title",
        { GroupConversationAvatar(color = Color.Green) },
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
        { GroupConversationAvatar(color = Color.Blue) },
        {},
        {},
        {},
        {},
    )
}
