/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.ui.home.conversationslist.bottomsheet

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.spec.DestinationStyle
import com.wire.android.R
import com.wire.android.model.ClickBlockParams
import com.wire.android.model.UserAvatarData
import com.wire.android.navigation.HomeNavGraph
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemIcon
import com.wire.android.ui.common.bottomsheet.MenuModalSheetContent
import com.wire.android.ui.common.bottomsheet.MenuModalSheetHeader
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.conversationColor
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.details.dialog.BlockUserDialog
import com.wire.android.ui.home.conversations.details.dialog.ClearContentDialog
import com.wire.android.ui.home.conversations.details.dialog.DeleteConversationDialog
import com.wire.android.ui.home.conversations.details.dialog.LeaveConversationDialog
import com.wire.android.ui.home.conversations.details.dialog.UnBlockUserDialog
import com.wire.android.ui.home.conversationslist.common.GroupConversationAvatar
import com.wire.android.ui.home.conversationslist.model.getMutedStatusTextResource
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.conversation.MutedConversationStatus

@HomeNavGraph
@Destination(
    navArgsDelegate = ConversationBottomSheetNavArg::class,
    style = DestinationStyle.BottomSheet::class
)
@Composable
fun ConversationBottomSheet(
// TODO(profile): enable when implemented
//
//    addConversationToFavourites: () -> Unit,
//    moveConversationToFolder: () -> Unit,
//    moveConversationToArchive: () -> Unit,
    navigateToNotification: () -> Unit = {},
    conversationBottomSheetViewModel: ConversationBottomSheetViewModel = hiltViewModel(),
    navigator: DestinationsNavigator,
) {

    with(conversationBottomSheetViewModel.dialogBottomSheetState) {
        if (showLeaveConversationDialog) {
            LeaveConversationDialog(
                conversationName = conversationBottomSheetViewModel.navArg.conversationName,
                isLoading = conversationBottomSheetViewModel.dialogBottomSheetState.isLoading,
                onDismiss = { conversationBottomSheetViewModel.dismissLeaveConversationDialog() },
                onLeave = {
                    conversationBottomSheetViewModel.leaveGroup {
                        navigator.navigateUp()
                    }
                }
            )
        }

        if (showDeleteConversationDialog) {
            DeleteConversationDialog(
                conversationName = conversationBottomSheetViewModel.navArg.conversationName,
                isLoading = conversationBottomSheetViewModel.dialogBottomSheetState.isLoading,
                onDismiss = { conversationBottomSheetViewModel.dismissDeleteConversationDialog() },
                onDelete = {
                    conversationBottomSheetViewModel.deleteGroup {
                        navigator.navigateUp()
                    }
                }
            )
        }

        if (showClearContentDialog) {
            val groupType = if (conversationBottomSheetViewModel.navArg.isGroup) R.string.group_label else R.string.conversation_label
            ClearContentDialog(
                groupType = groupType,
                isLoading = conversationBottomSheetViewModel.dialogBottomSheetState.isLoading,
                onDismiss = { conversationBottomSheetViewModel.dismissClearContentDialog() },
                onClear = {
                    conversationBottomSheetViewModel.clearConversationContent {
                        navigator.navigateUp()
                    }
                }
            )
        }

        if (showBlockUserDialog) {
            BlockUserDialog(
                isLoading = conversationBottomSheetViewModel.dialogBottomSheetState.isLoading,
                userName = conversationBottomSheetViewModel.navArg.conversationName,
                onDismiss = { conversationBottomSheetViewModel.dismissBlockUserDialog() },
                onBlock = {
                    conversationBottomSheetViewModel.blockUser {
                        navigator.navigateUp()
                    }
                }
            )
        }

        if (showUnblockUserDialog) {
            UnBlockUserDialog(
                isLoading = conversationBottomSheetViewModel.dialogBottomSheetState.isLoading,
                userName = conversationBottomSheetViewModel.navArg.conversationName,
                onDismiss = { conversationBottomSheetViewModel.dismissUnBlockUserDialog() },
                onBlock = {
                    conversationBottomSheetViewModel.unblockUser {
                        navigator.navigateUp()
                    }
                }
            )
        }
    }

    with(conversationBottomSheetViewModel.navArg) {
        MenuModalSheetContent(
            header = MenuModalSheetHeader.Visible(
                title = conversationName,
                leadingIcon = {
                    assetId?.let {
                        UserProfileAvatar(
                            avatarData = UserAvatarData(
                                asset = conversationBottomSheetViewModel.loadAvatar(assetId),
                                connectionState = connectionState
                            )
                        )
                    } ?: run {
                        GroupConversationAvatar(
                            color = colorsScheme().conversationColor(id = conversationId)
                        )
                    }
                },
                customVerticalPadding = dimensions().spacing8x
            ),
            menuItems = buildList<@Composable () -> Unit> {
                if (canEditConversation) {
                    add {
                        MenuBottomSheetItem(
                            title = stringResource(R.string.label_notifications),
                            icon = {
                                MenuItemIcon(
                                    id = R.drawable.ic_mute,
                                    contentDescription = stringResource(R.string.content_description_muted_conversation),
                                )
                            },
                            action = { NotificationsOptionsItemAction(mutingConversationState) },
                            onItemClick = {

                            }
                        )
                    }
                }
// TODO(profile): enable when implemented
//
//            if (conversationSheetContent.canAddToFavourite())
//                add {
//                    MenuBottomSheetItem(
//                        title = stringResource(R.string.label_add_to_favourites),
//                        icon = {
//                            MenuItemIcon(
//                                id = R.drawable.ic_favourite,
//                                contentDescription = stringResource(R.string.content_description_add_to_favourite),
//                            )
//                        },
//                        onItemClick = addConversationToFavourites
//                    )
//                }
//            add {
//                MenuBottomSheetItem(
//                    icon = {
//                        MenuItemIcon(
//                            id = R.drawable.ic_folder,
//                            contentDescription = stringResource(R.string.content_description_move_to_folder),
//                        )
//                    },
//                    title = stringResource(R.string.label_move_to_folder),
//                    onItemClick = moveConversationToFolder
//                )
//            }
//            add {
//                MenuBottomSheetItem(
//                    icon = {
//                        MenuItemIcon(
//                            id = R.drawable.ic_archive,
//                            contentDescription = stringResource(R.string.content_description_move_to_archive),
//                        )
//                    },
//                    title = stringResource(R.string.label_move_to_archive),
//                    onItemClick = moveConversationToArchive
//                )
//            }
                add {
                    MenuBottomSheetItem(
                        icon = {
                            MenuItemIcon(
                                id = R.drawable.ic_erase,
                                contentDescription = stringResource(R.string.content_description_clear_content),
                            )
                        },
                        title = stringResource(R.string.label_clear_content),
                        onItemClick = {
                            conversationBottomSheetViewModel.showClearContentDialog()
                        }
                    )
                }
                if (canBlockUser) {
                    add {
                        MenuBottomSheetItem(
                            icon = {
                                MenuItemIcon(
                                    id = R.drawable.ic_block,
                                    contentDescription = stringResource(R.string.content_description_block_the_user),
                                )
                            },
                            itemProvidedColor = MaterialTheme.colorScheme.error,
                            title = stringResource(R.string.label_block),
                            clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
                            onItemClick = {
                                conversationBottomSheetViewModel.showBlockUserDialog()
                            }
                        )
                    }
                }
                if (canUnblockUser) {
                    add {
                        MenuBottomSheetItem(
                            icon = {
                                MenuItemIcon(
                                    id = R.drawable.ic_block,
                                    contentDescription = stringResource(R.string.content_description_unblock_the_user)
                                )
                            },
                            itemProvidedColor = MaterialTheme.colorScheme.onBackground,
                            title = stringResource(R.string.label_unblock),
                            onItemClick = {
                                conversationBottomSheetViewModel.showUnBlockUserDialog()
                            }
                        )
                    }
                }
                if (canLeaveGroup) {
                    add {
                        MenuBottomSheetItem(
                            icon = {
                                MenuItemIcon(
                                    id = R.drawable.ic_leave,
                                    contentDescription = stringResource(R.string.content_description_leave_the_group),
                                )
                            },
                            itemProvidedColor = MaterialTheme.colorScheme.error,
                            title = stringResource(R.string.label_leave_group),
                            onItemClick = {
                                conversationBottomSheetViewModel.showLeaveConversationDialog()
                            }
                        )
                    }
                }
                if (canDeleteGroup) {
                    add {
                        MenuBottomSheetItem(
                            icon = {
                                MenuItemIcon(
                                    id = R.drawable.ic_remove,
                                    contentDescription = stringResource(R.string.content_description_delete_the_group),
                                )
                            },
                            title = stringResource(R.string.label_delete_group),
                            itemProvidedColor = MaterialTheme.colorScheme.error,
                            onItemClick = {
                                conversationBottomSheetViewModel.showDeleteConversationDialog()
                            }
                        )
                    }
                }
            }
        )
    }

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
