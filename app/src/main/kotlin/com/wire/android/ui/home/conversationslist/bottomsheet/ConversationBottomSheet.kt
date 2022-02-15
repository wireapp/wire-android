package com.wire.android.ui.home.conversationslist.bottomsheet

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.wire.android.R
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.bottomsheet.MenuModalSheetLayout
import com.wire.android.ui.common.bottomsheet.ModalBottomSheetItem
import com.wire.android.ui.home.conversations.common.GroupConversationAvatar
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import io.github.esentsov.PackagePrivate


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ConversationSheet(
    sheetState: ModalBottomSheetState,
    modalBottomSheetContentState: ModalSheetContent,
    muteConversation: (String) -> Unit,
    addConversationToFavourites: (String) -> Unit,
    moveConversationToFolder: (String) -> Unit,
    moveConversationToArchive: (String) -> Unit,
    clearConversationContent: (String) -> Unit,
    blockUser: (String) -> Unit,
    leaveGroup: (String) -> Unit,
    content: @Composable () -> Unit
) {
    MenuModalSheetLayout(
        sheetState = sheetState,
        headerTitle = modalBottomSheetContentState.title,
        headerIcon = {
            if (modalBottomSheetContentState is ModalSheetContent.GroupConversationEdit) {
                GroupConversationAvatar(colorValue = modalBottomSheetContentState.groupColorValue)
            } else {
                UserProfileAvatar()
            }
        },
        menuItems = listOf(
            {
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
            },
            {
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
            },
            {
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
            },
            {
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
            },
            {
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
            },
            {
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
                } else {
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
        )
    ) {
        content()
    }
}


@PackagePrivate
@Composable
fun ItemIcon(
    @DrawableRes id: Int,
    contentDescription: String,
    size: Dp = MaterialTheme.wireDimensions.conversationBottomSheetItemSize,
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

sealed class ModalSheetContent(val title: String) {
    object Initial : ModalSheetContent("")
    class PrivateConversationEdit(title: String, val avatarUrl: String) : ModalSheetContent(title)
    class GroupConversationEdit(title: String, val groupColorValue: Long) : ModalSheetContent(title)
}
