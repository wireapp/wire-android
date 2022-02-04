package com.wire.android.ui.home.conversationslist.bottomsheet

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.home.conversations.common.GroupConversationAvatar
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography


@Composable
fun PrivateConversationSheet(
    content: ModalSheetContent.PrivateConversationEdit,
    onMuteClick: () -> Unit,
    onAddToFavouritesClick: () -> Unit,
    onMoveToFolderClick: () -> Unit,
    onMoveToArchiveClick: () -> Unit,
    onClearContentClick: () -> Unit,
    onBlockClick: () -> Unit
) {
    PrivateConversationHeader(content)
    PrivateConversationItems(
        onMuteClick = onMuteClick,
        onAddToFavouritesClick = onAddToFavouritesClick,
        onMoveToFolderClick = onMoveToFolderClick,
        onMoveToArchiveClick = onMoveToArchiveClick,
        onClearContentClick = onClearContentClick,
        onBlockClick = onBlockClick,
    )
}

@Composable
private fun PrivateConversationHeader(content: ModalSheetContent.PrivateConversationEdit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(
            start = 8.dp,
            top = 16.dp,
            bottom = 8.dp
        )
    ) {
        UserProfileAvatar()
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = content.title,
            style = MaterialTheme.wireTypography.body02
        )
    }
}


@Composable
fun GroupConversationSheet(
    content: ModalSheetContent.GroupConversationEdit, onMuteClick: () -> Unit,
    onAddToFavouritesClick: () -> Unit,
    onMoveToFolderClick: () -> Unit,
    onMoveToArchiveClick: () -> Unit,
    onClearContentClick: () -> Unit,
    onLeaveClick: () -> Unit
) {
    with(content) {
        GroupHeader(
            title = title,
            groupColorValue = groupColorValue
        )
    }
    GroupConversationItems(
        onMuteClick = onMuteClick,
        onAddToFavouritesClick = onAddToFavouritesClick,
        onMoveToFolderClick = onMoveToFolderClick,
        onMoveToArchiveClick = onMoveToArchiveClick,
        onClearContentClick = onClearContentClick,
        onLeaveClick = onLeaveClick,
    )
}

@Composable
fun GroupHeader(title: String, groupColorValue: Long) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(
            start = 8.dp,
            top = 16.dp,
            bottom = 8.dp
        )
    ) {
        GroupConversationAvatar(colorValue = groupColorValue)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.wireTypography.body02
        )
    }
}

@Composable
fun ItemIcon(
    @DrawableRes id: Int,
    contentDescription: String,
    size: Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    Icon(
        painter = painterResource(id = id),
        contentDescription = contentDescription,
        modifier = Modifier
            .size(size)
            .then(modifier)
    )
}

@Composable
fun ItemTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.wireTypography.body01,
        modifier = modifier
    )
}

@Composable
fun ModalBottomSheetItem(
    icon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    onItemClick: () -> Unit = {}
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
        .height(MaterialTheme.wireDimensions.conversationBottomSheetItemHeight)
        .fillMaxWidth()
        .clickable { onItemClick() }
        .padding(MaterialTheme.wireDimensions.conversationBottomSheetItemPadding)
    ) {
        icon()
        Spacer(modifier = Modifier.width(12.dp))
        title()
    }
}

@Preview
@Composable
private fun PreviewPrivateModalSheet() {
    PrivateConversationSheet(
        content = ModalSheetContent.PrivateConversationEdit("Some test title for the conversation", ""),
        onMuteClick = { },
        onAddToFavouritesClick = { },
        onMoveToFolderClick = { },
        onMoveToArchiveClick = { },
        onClearContentClick = { },
        onBlockClick = { }
    )
}

@Preview
@Composable
private fun PreviewGroupModalSheet() {
    GroupConversationSheet(
        content = ModalSheetContent.GroupConversationEdit("Some test title for the conversation", 0xFF00FFFF),
        onMuteClick = { },
        onAddToFavouritesClick = { },
        onMoveToFolderClick = { },
        onMoveToArchiveClick = { },
        onClearContentClick = { },
        onLeaveClick = { }
    )
}

sealed class ModalSheetContent {
    object Initial : ModalSheetContent()
    data class PrivateConversationEdit(val title: String, val avatarUrl: String) : ModalSheetContent()
    data class GroupConversationEdit(val title: String, val groupColorValue: Long) : ModalSheetContent()
}
