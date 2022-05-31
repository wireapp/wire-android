package com.wire.android.ui.home.conversationslist.bottomsheet

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemIcon
import com.wire.android.ui.common.bottomsheet.MenuModalSheetContent
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.conversationColor
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversationslist.common.GroupConversationAvatar
import com.wire.android.ui.home.conversationslist.model.getMutedStatusTextResource
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.conversation.MutedConversationStatus

@Composable
internal fun HomeSheetContent(
    conversationSheetContent: ConversationSheetContent,
    addConversationToFavourites: () -> Unit,
    moveConversationToFolder: () -> Unit,
    moveConversationToArchive: () -> Unit,
    clearConversationContent: () -> Unit,
    blockUser: () -> Unit,
    leaveGroup: () -> Unit,
    navigateToNotification : () -> Unit,
) {
    MenuModalSheetContent(
        headerTitle = conversationSheetContent.title,
        headerIcon = {
            if (conversationSheetContent.conversationTypeDetail is ConversationTypeDetail.Group) {
                GroupConversationAvatar(
                    color = colorsScheme()
                        .conversationColor(id = conversationSheetContent.conversationTypeDetail.conversationId)
                )
            } else if (conversationSheetContent.conversationTypeDetail is ConversationTypeDetail.Private) {
                UserProfileAvatar(userAvatarAsset = conversationSheetContent.conversationTypeDetail.avatarAsset)
            }
        },
        menuItems = listOf(
            {
                MenuBottomSheetItem(
                    title = stringResource(R.string.label_notifications),
                    icon = {
                        MenuItemIcon(
                            id = R.drawable.ic_mute,
                            contentDescription = stringResource(R.string.content_description_mute),
                        )
                    },
                    action = { NotificationsOptionsItemAction(conversationSheetContent.mutingConversationState) },
                    onItemClick = navigateToNotification
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
                if (conversationSheetContent.conversationTypeDetail is ConversationTypeDetail.Private) {
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
    )
}

@Composable
internal fun NotificationsOptionsItemAction(
    mutedStatus: MutedConversationStatus
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = mutedStatus.getMutedStatusTextResource(),
            style = MaterialTheme.wireTypography.body01
        )
        Spacer(modifier = Modifier.size(dimensions().spacing16x))
        ArrowRightIcon()
    }
}

