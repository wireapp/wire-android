package com.wire.android.ui.home.conversationslist.bottomsheet

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.ExperimentalMaterialApi
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.home.conversations.common.GroupConversationAvatar
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import io.github.esentsov.PackagePrivate

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ColumnScope.ConversationSheet(
    modalBottomSheetContentState: ModalSheetContent,
    muteConversation: (String) -> Unit,
    addConversationToFavourites: (String) -> Unit,
    moveConversationToFolder: (String) -> Unit,
    moveConversationToArchive: (String) -> Unit,
    clearConversationContent: (String) -> Unit,
    blockUser: (String) -> Unit,
    leaveGroup: (String) -> Unit
) {
    Spacer(modifier = Modifier.height(8.dp))
    Divider(
        modifier = Modifier
            .width(width = 48.dp)
            .align(alignment = Alignment.CenterHorizontally),
        thickness = 4.dp
    )

    if (modalBottomSheetContentState is ModalSheetContent.GroupConversationEdit) {
        GroupHeader(
            title = modalBottomSheetContentState.title,
            groupColorValue = modalBottomSheetContentState.groupColorValue
        )
    } else if (modalBottomSheetContentState is ModalSheetContent.PrivateConversationEdit) {
        PrivateConversationHeader(content = modalBottomSheetContentState)
    }

    ModalBottomSheetItem(
        icon = {
            ItemIcon(
                id = R.drawable.ic_mute,
                contentDescription = stringResource(R.string.content_description_mute),
            )
        },
        title = { ItemTitle(stringResource(R.string.label_mute)) },
        { muteConversation("someId") }
    )
    Divider()
    ModalBottomSheetItem(
        icon = {
            ItemIcon(
                id = R.drawable.ic_favourite,
                contentDescription = stringResource(R.string.content_description_add_to_favourite),
            )
        },
        title = { ItemTitle(stringResource(R.string.label_add_to_favourites)) },
        { addConversationToFavourites("someId") }
    )
    Divider()
    ModalBottomSheetItem(
        icon = {
            ItemIcon(
                id = R.drawable.ic_folder,
                contentDescription = stringResource(R.string.content_description_move_to_folder),
            )
        },
        title = { ItemTitle(stringResource(R.string.label_move_to_folder)) },
        { moveConversationToFolder("someId") }
    )
    Divider()
    ModalBottomSheetItem(
        icon = {
            ItemIcon(
                id = R.drawable.ic_archive,
                contentDescription = stringResource(R.string.content_description_move_to_archive),
            )
        },
        title = { ItemTitle(stringResource(R.string.label_move_to_archive)) },
        { moveConversationToArchive("someId") }
    )
    Divider()
    ModalBottomSheetItem(
        icon = {
            ItemIcon(
                id = R.drawable.ic_erase,
                contentDescription = stringResource(R.string.content_description_clear_content),
            )
        },
        title = { ItemTitle(stringResource(R.string.label_clear_content)) },
        { clearConversationContent("someId") }
    )
    Divider()
    if (modalBottomSheetContentState is ModalSheetContent.PrivateConversationEdit) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) {
            ModalBottomSheetItem(
                icon = {
                    ItemIcon(
                        id = R.drawable.ic_block,
                        contentDescription = stringResource(R.string.content_description_block_the_user),
                    )
                },
                title = { ItemTitle(stringResource(R.string.label_block)) },
                { blockUser("someId") }
            )
        }
    } else if (modalBottomSheetContentState is ModalSheetContent.GroupConversationEdit) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) {
            ModalBottomSheetItem(
                icon = {
                    ItemIcon(
                        id = R.drawable.ic_leave,
                        contentDescription = stringResource(R.string.content_description_leave_the_group),
                    )
                },
                title = { ItemTitle(stringResource(R.string.label_leave_group)) },
                { leaveGroup("someId") }
            )
        }
    }
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

@PackagePrivate
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

@PackagePrivate
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

@PackagePrivate
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

@PackagePrivate
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

sealed class ModalSheetContent {
    object Initial : ModalSheetContent()
    data class PrivateConversationEdit(val title: String, val avatarUrl: String) : ModalSheetContent()
    data class GroupConversationEdit(val title: String, val groupColorValue: Long) : ModalSheetContent()
}
