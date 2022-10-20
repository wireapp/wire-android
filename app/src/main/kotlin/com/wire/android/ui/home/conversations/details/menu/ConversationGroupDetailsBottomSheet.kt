package com.wire.android.ui.home.conversations.details.menu

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.bottomsheet.MenuModalSheetContent
import com.wire.android.ui.common.bottomsheet.MenuModalSheetHeader
import com.wire.android.ui.common.bottomsheet.SelectableMenuBottomSheetItem
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.conversationColor
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsState
import com.wire.android.ui.home.conversationslist.common.GroupConversationAvatar
import com.wire.android.ui.home.conversationslist.model.GroupDialogState

@Composable
fun ConversationGroupDetailsBottomSheet(
    conversationOptionsState: GroupConversationOptionsState,
    onDeleteGroup: (GroupDialogState) -> Unit,
    onLeaveGroup: (GroupDialogState) -> Unit,
    closeBottomSheet: () -> Unit
) {
    MenuModalSheetContent(
        header = MenuModalSheetHeader.Visible(
            title = conversationOptionsState.groupName,
            leadingIcon = {
                GroupConversationAvatar(
                    color = colorsScheme().conversationColor(
                        id =
                        conversationOptionsState.conversationId
                    )
                )
            }),
        menuItems = listOf<@Composable () -> Unit>().also {
            if (conversationOptionsState.isSelfUserMember) it.plus {
                LeaveGroupItem(
                    onLeaveGroup = {
                        onLeaveGroup(
                            GroupDialogState(
                                conversationOptionsState.conversationId,
                                conversationOptionsState.groupName
                            )
                        )
                    },
                    closeBottomSheet = closeBottomSheet
                )
            }
            if (conversationOptionsState.isAbleToRemoveGroup) it.plus {
                DeleteGroupItem(
                    onDeleteGroup = {
                        onDeleteGroup(
                            GroupDialogState(
                                conversationOptionsState.conversationId,
                                conversationOptionsState.groupName
                            )
                        )
                    },
                    closeBottomSheet = closeBottomSheet
                )
            }
        },
    )
}

@Composable
private fun LeaveGroupItem(
    onLeaveGroup: () -> Unit,
    closeBottomSheet: () -> Unit
) {
    SelectableMenuBottomSheetItem(
        title = stringResource(id = R.string.leave_group_conversation_menu_item),
        titleColor = MaterialTheme.colorScheme.error,
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_leave),
                contentDescription = stringResource(R.string.content_description_leave_the_group),
                modifier = Modifier.padding(dimensions().spacing8x),
                tint = MaterialTheme.colorScheme.error
            )
        },
        onItemClick = Clickable(blockUntilSynced = true) {
            onLeaveGroup()
            closeBottomSheet()
        }
    )
}

@Composable
private fun DeleteGroupItem(
    onDeleteGroup: () -> Unit,
    closeBottomSheet: () -> Unit
) {
    SelectableMenuBottomSheetItem(
        title = stringResource(id = R.string.delete_group_conversation_menu_item),
        titleColor = MaterialTheme.colorScheme.error,
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_remove),
                contentDescription = stringResource(R.string.content_description_delete_the_group),
                modifier = Modifier.padding(dimensions().spacing8x),
                tint = MaterialTheme.colorScheme.error
            )
        },
        onItemClick = Clickable {
            onDeleteGroup()
            closeBottomSheet()
        }
    )
}
