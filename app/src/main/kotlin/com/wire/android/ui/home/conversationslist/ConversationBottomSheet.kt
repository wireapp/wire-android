package com.wire.android.ui.home.conversationslist

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.home.conversations.common.GroupConversationAvatar
import com.wire.android.ui.theme.wireTypography


private fun commonConversationItems(
    onMuteClick: () -> Unit,
    onAddToFavouritesClick: () -> Unit,
    onMoveToFolderClick: () -> Unit,
    onMoveToArchiveClick: () -> Unit,
    onClearContentClick: () -> Unit
): List<@Composable () -> Unit> {
    return listOf<@Composable () -> Unit>(
        {
            ModalBottomSheetItem(
                icon = {
                    ItemIcon(
                        id = R.drawable.ic_mute,
                        contentDescription = "",
                    )
                },
                title = { ItemTitle("Mute") },
                onMuteClick
            )
        },
        {
            ModalBottomSheetItem(
                icon = {
                    ItemIcon(
                        id = R.drawable.ic_favourite,
                        contentDescription = "",
                    )
                },
                title = { ItemTitle("Add to Favourites") },
                onAddToFavouritesClick
            )
        },
        {
            ModalBottomSheetItem(
                icon = {
                    ItemIcon(
                        id = R.drawable.ic_folder,
                        contentDescription = "",
                    )
                },
                title = { ItemTitle("Move to Folder") },
                onMoveToFolderClick
            )
        },
        {
            ModalBottomSheetItem(
                icon = {
                    ItemIcon(
                        id = R.drawable.ic_archive,
                        contentDescription = "",
                    )
                },
                title = { ItemTitle("Move to Archive") },
                onMoveToArchiveClick
            )
        }, {
            ModalBottomSheetItem(
                icon = {
                    ItemIcon(
                        id = R.drawable.ic_erase,
                        contentDescription = "",
                    )
                },
                title = { ItemTitle("Clear Content...") },
                onClearContentClick
            )
        }
    )
}

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
private fun PrivateConversationItems(
    onMuteClick: () -> Unit,
    onAddToFavouritesClick: () -> Unit,
    onMoveToFolderClick: () -> Unit,
    onMoveToArchiveClick: () -> Unit,
    onClearContentClick: () -> Unit,
    onBlockClick: () -> Unit
) {
    buildItems(
        items = privateConversationItems(
            onMuteClick = onMuteClick,
            onAddToFavouritesClick = onAddToFavouritesClick,
            onMoveToFolderClick = onMoveToFolderClick,
            onMoveToArchiveClick = onMoveToArchiveClick,
            onClearContentClick = onClearContentClick,
            onBlockClick = onBlockClick,
        )
    )
}

private fun privateConversationItems(
    onMuteClick: () -> Unit,
    onAddToFavouritesClick: () -> Unit,
    onMoveToFolderClick: () -> Unit,
    onMoveToArchiveClick: () -> Unit,
    onClearContentClick: () -> Unit,
    onBlockClick: () -> Unit
): List<@Composable () -> Unit> {
    return commonConversationItems(
        onMuteClick = onMuteClick,
        onAddToFavouritesClick = onAddToFavouritesClick,
        onMoveToFolderClick = onMoveToFolderClick,
        onMoveToArchiveClick = onMoveToArchiveClick,
        onClearContentClick = onClearContentClick
    ) + {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) {
            ModalBottomSheetItem(
                icon = {
                    ItemIcon(
                        id = R.drawable.ic_leave,
                        contentDescription = "",
                    )
                },
                title = { ItemTitle("Block") },
                onBlockClick
            )
        }
    }
}

@Composable
 fun GroupConversationSheet(
    groupConversationEdit: ModalSheetContent.GroupConversationEdit, onMuteClick: () -> Unit,
    onAddToFavouritesClick: () -> Unit,
    onMoveToFolderClick: () -> Unit,
    onMoveToArchiveClick: () -> Unit,
    onClearContentClick: () -> Unit,
    onLeaveClick: () -> Unit
) {
    with(groupConversationEdit) {
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
private fun GroupConversationItems(
    onMuteClick: () -> Unit,
    onAddToFavouritesClick: () -> Unit,
    onMoveToFolderClick: () -> Unit,
    onMoveToArchiveClick: () -> Unit,
    onClearContentClick: () -> Unit,
    onLeaveClick: () -> Unit
) {
    buildItems(
        items = groupConversationItems(
            onMuteClick = onMuteClick,
            onAddToFavouritesClick = onAddToFavouritesClick,
            onMoveToFolderClick = onMoveToFolderClick,
            onMoveToArchiveClick = onMoveToArchiveClick,
            onClearContentClick = onClearContentClick,
            onLeaveClick = onLeaveClick,
        )
    )
}

private fun groupConversationItems(
    onMuteClick: () -> Unit,
    onAddToFavouritesClick: () -> Unit,
    onMoveToFolderClick: () -> Unit,
    onMoveToArchiveClick: () -> Unit,
    onClearContentClick: () -> Unit,
    onLeaveClick: () -> Unit
): List<@Composable () -> Unit> {
    return commonConversationItems(
        onMuteClick = onMuteClick,
        onAddToFavouritesClick = onAddToFavouritesClick,
        onMoveToFolderClick = onMoveToFolderClick,
        onMoveToArchiveClick = onMoveToArchiveClick,
        onClearContentClick = onClearContentClick
    ) + {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) {
            ModalBottomSheetItem(
                icon = {
                    ItemIcon(
                        id = R.drawable.ic_leave,
                        contentDescription = "",
                    )
                },
                title = { ItemTitle("Leave Group") },
                onLeaveClick
            )
        }
    }
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
 fun buildItems(items: List<@Composable () -> Unit>) {
    items.forEachIndexed { index, sheetItem ->
        sheetItem()
        if (index != items.size) {
            Divider()
        }
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
        .height(48.dp)
        .fillMaxWidth()
        .clickable { onItemClick() }
        .padding(16.dp)
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
