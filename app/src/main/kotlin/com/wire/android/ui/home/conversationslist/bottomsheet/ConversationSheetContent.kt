package com.wire.android.ui.home.conversationslist.bottomsheet

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.model.UserAvatarAsset
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemIcon
import com.wire.android.ui.common.bottomsheet.MenuModalSheetContent
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.common.GroupConversationAvatar
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ConversationSheetContent(
    modalBottomSheetContentState: ModalSheetContent,
    notificationsOptionsItem: NotificationsOptionsItem,
    addConversationToFavourites: () -> Unit,
    moveConversationToFolder: () -> Unit,
    moveConversationToArchive: () -> Unit,
    clearConversationContent: () -> Unit,
    blockUser: () -> Unit,
    leaveGroup: () -> Unit,
) {
    MenuModalSheetContent(
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
                    title = stringResource(R.string.label_notifications),
                    icon = {
                        MenuItemIcon(
                            id = R.drawable.ic_mute,
                            contentDescription = stringResource(R.string.content_description_mute),
                        )
                    },
                    action = { NotificationsOptionsItemAction(notificationsOptionsItem.mutedStatus) },
                    onItemClick = notificationsOptionsItem.muteConversationAction
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
    )
}

@Composable
fun NotificationsOptionsItemAction(
    mutedStatus: MutedConversationStatus
) {
    Text(text = "${mutedStatus.status}")
    Spacer(modifier = Modifier.size(dimensions().spacing2x))
    ArrowRightIcon()
}

sealed class ModalSheetContent(val title: String, val conversationId: ConversationId?, var mutedStatus: MutedConversationStatus) {
    object Initial : ModalSheetContent("", null, MutedConversationStatus.AllAllowed)
    class PrivateConversationEdit(
        title: String,
        val avatarAsset: UserAvatarAsset?,
        conversationId: ConversationId,
        mutedStatus: MutedConversationStatus
    ) :
        ModalSheetContent(title, conversationId, mutedStatus)

    class GroupConversationEdit(
        title: String,
        val groupColorValue: Long,
        conversationId: ConversationId,
        mutedStatus: MutedConversationStatus
    ) :
        ModalSheetContent(title, conversationId, mutedStatus)
}

data class NotificationsOptionsItem(
    val muteConversationAction: () -> Unit,
    val mutedStatus: MutedConversationStatus
)
