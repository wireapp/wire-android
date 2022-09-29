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
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.R
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemIcon
import com.wire.android.ui.common.bottomsheet.MenuModalSheetContent
import com.wire.android.ui.common.bottomsheet.MenuModalSheetHeader
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.conversationColor
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversationslist.common.GroupConversationAvatar
import com.wire.android.ui.home.conversationslist.model.BlockingState
import com.wire.android.ui.home.conversationslist.model.GroupDialogState
import com.wire.android.ui.home.conversationslist.model.getMutedStatusTextResource
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.user.ConnectionState

@Composable
internal fun ConversationMainSheetContent(
    conversationSheetContent: ConversationSheetContent,
// TODO(profile): enable when implemented
//
//    addConversationToFavourites: () -> Unit,
//    moveConversationToFolder: () -> Unit,
//    moveConversationToArchive: () -> Unit,
//    clearConversationContent: () -> Unit,
    blockUserClick: (BlockUserDialogState) -> Unit,
    leaveGroup: (GroupDialogState) -> Unit,
    deleteGroup: (GroupDialogState) -> Unit,
    navigateToNotification: () -> Unit
) {
    MenuModalSheetContent(
        header = MenuModalSheetHeader.Visible(
            title = conversationSheetContent.title,
            leadingIcon = {
                    if (conversationSheetContent.conversationTypeDetail is ConversationTypeDetail.Group) {
                    GroupConversationAvatar(
                        color = colorsScheme()
                            .conversationColor(id = conversationSheetContent.conversationTypeDetail.conversationId)
                    )
                } else if (conversationSheetContent.conversationTypeDetail is ConversationTypeDetail.Private) {
                    val connectionState: ConnectionState? = conversationSheetContent.conversationTypeDetail.blockingState.let {
                        if (it == BlockingState.BLOCKED) ConnectionState.BLOCKED else null
                    }
                    UserProfileAvatar(
                        avatarData = UserAvatarData(
                            asset = conversationSheetContent.conversationTypeDetail.avatarAsset,
                            connectionState = connectionState
                        )
                    )
                }
            },
            customBottomPadding = dimensions().spacing8x
        ),
        menuItems = listOf(
            {
                if (conversationSheetContent.isSelfUserMember)
                    MenuBottomSheetItem(
                        title = stringResource(R.string.label_notifications),
                        icon = {
                            MenuItemIcon(
                                id = R.drawable.ic_mute,
                                contentDescription = stringResource(R.string.content_description_muted_conversation),
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
                    onItemClick = {  }
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
                    onItemClick = {  }
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
                    onItemClick = {  }
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
                    onItemClick = {  }
                )
            },
            {
                if (conversationSheetContent.conversationTypeDetail is ConversationTypeDetail.Private) {
                    if (conversationSheetContent.conversationTypeDetail.blockingState == BlockingState.NOT_BLOCKED) {
                        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) {
                            MenuBottomSheetItem(
                                icon = {
                                    MenuItemIcon(
                                        id = R.drawable.ic_block,
                                        contentDescription = stringResource(R.string.content_description_block_the_user),
                                    )
                                },
                                title = stringResource(R.string.label_block),
                                onItemClick = {
                                    blockUserClick(
                                        BlockUserDialogState(
                                            userName = conversationSheetContent.title,
                                            userId = conversationSheetContent.conversationTypeDetail.userId
                                        )
                                    )
                                }
                            )
                        }
                    }
                } else if (conversationSheetContent.isSelfUserMember) {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) {
                        MenuBottomSheetItem(
                            icon = {
                                MenuItemIcon(
                                    id = R.drawable.ic_leave,
                                    contentDescription = stringResource(R.string.content_description_leave_the_group),
                                )
                            },
                            title = stringResource(R.string.label_leave_group),
                            onItemClick = {
                                leaveGroup(
                                    GroupDialogState(
                                        conversationSheetContent.conversationId,
                                        conversationSheetContent.title
                                    )
                                )
                            }
                        )
                    }
                }
            },
            {
                val conversationTypeDetail = conversationSheetContent.conversationTypeDetail
                if (conversationTypeDetail is ConversationTypeDetail.Group && conversationTypeDetail.isCreator) {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) {
                        MenuBottomSheetItem(
                            icon = {
                                MenuItemIcon(
                                    id = R.drawable.ic_remove,
                                    contentDescription = stringResource(R.string.content_description_delete_the_group),
                                )
                            },
                            title = stringResource(R.string.label_delete_group),
                            onItemClick = {
                                deleteGroup(
                                    GroupDialogState(
                                        conversationSheetContent.conversationId,
                                        conversationSheetContent.title
                                    )
                                )
                            }
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = mutedStatus.getMutedStatusTextResource(),
            style = MaterialTheme.wireTypography.body01,
            color = MaterialTheme.wireColorScheme.secondaryText,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier.weight(weight = 1f, fill = false)
        )
        Spacer(modifier = Modifier.size(dimensions().spacing16x))
        ArrowRightIcon()
    }
}

