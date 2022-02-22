package com.wire.android.ui.home.conversationslist.bottomsheet

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemIcon
import com.wire.android.ui.common.bottomsheet.MenuModalSheetLayout
import com.wire.android.ui.home.conversations.common.GroupConversationAvatar


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ConversationSheetContent(
    sheetState: ModalBottomSheetState,
    modalBottomSheetContentState: ModalSheetContent,
    muteConversation: () -> Unit,
    addConversationToFavourites: () -> Unit,
    moveConversationToFolder: () -> Unit,
    moveConversationToArchive: () -> Unit,
    clearConversationContent: () -> Unit,
    blockUser: () -> Unit,
    leaveGroup: () -> Unit,
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
                MenuBottomSheetItem(
                    title = stringResource(R.string.label_mute),
                    icon = {
                        MenuItemIcon(
                            id = R.drawable.ic_mute,
                            contentDescription = stringResource(R.string.content_description_mute),
                        )
                    },
                    onItemClick = muteConversation
                )
            },
            {
                MenuBottomSheetItem(
                    title = stringResource(R.string.label_add_to_favourites),
                    icon = {
                        MenuItemIcon(
                            id = R.drawable.ic_favourite,
                            contentDescription = stringResource(R.string.content_description_add_to_favourite),
                        )
                    },
                    onItemClick = addConversationToFavourites
                )
            },
            {
                MenuBottomSheetItem(
                    icon = {
                        MenuItemIcon(
                            id = R.drawable.ic_folder,
                            contentDescription = stringResource(R.string.content_description_move_to_folder),
                        )
                    },
                    title = stringResource(R.string.label_move_to_folder),
                    onItemClick = moveConversationToFolder
                )
            },
            {
                MenuBottomSheetItem(
                    icon = {
                        MenuItemIcon(
                            id = R.drawable.ic_archive,
                            contentDescription = stringResource(R.string.content_description_move_to_archive),
                        )
                    },
                    title = stringResource(R.string.label_move_to_archive),
                    onItemClick = moveConversationToArchive
                )
            },
            {
                MenuBottomSheetItem(
                    icon = {
                        MenuItemIcon(
                            id = R.drawable.ic_erase,
                            contentDescription = stringResource(R.string.content_description_clear_content),
                        )
                    },
                    title = stringResource(R.string.label_clear_content),
                    onItemClick = clearConversationContent
                )
            },
            {
                if (modalBottomSheetContentState is ModalSheetContent.PrivateConversationEdit) {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) {
                        MenuBottomSheetItem(
                            icon = {
                                MenuItemIcon(
                                    id = R.drawable.ic_block,
                                    contentDescription = stringResource(R.string.content_description_block_the_user),
                                )
                            },
                            title = stringResource(R.string.label_block),
                            onItemClick = blockUser
                        )
                    }
                } else {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) {
                        MenuBottomSheetItem(
                            icon = {
                                MenuItemIcon(
                                    id = R.drawable.ic_leave,
                                    contentDescription = stringResource(R.string.content_description_leave_the_group),
                                )
                            },
                            title = stringResource(R.string.label_leave_group),
                            onItemClick = leaveGroup
                        )
                    }
                }
            }
        )
    ) {
        content()
    }
}


sealed class ModalSheetContent(val title: String) {
    object Initial : ModalSheetContent("")
    class PrivateConversationEdit(title: String, val avatarUrl: String) : ModalSheetContent(title)
    class GroupConversationEdit(title: String, val groupColorValue: Long) : ModalSheetContent(title)
}
