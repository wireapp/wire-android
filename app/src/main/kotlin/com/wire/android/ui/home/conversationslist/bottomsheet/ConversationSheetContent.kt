package com.wire.android.ui.home.conversationslist.bottomsheet

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemIcon
import com.wire.android.ui.common.bottomsheet.MenuModalSheetContent
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversationslist.model.getMutedStatusTextResource
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.android.ui.home.conversationslist.common.GroupConversationAvatar

@Composable
fun ConversationSheetContent(
    conversationSheetContent: ConversationSheetContent,
    mutedStatus: MutedConversationStatus,
    muteConversation: () -> Unit,
    addConversationToFavourites: () -> Unit,
    moveConversationToFolder: () -> Unit,
    moveConversationToArchive: () -> Unit,
    clearConversationContent: () -> Unit,
    blockUser: () -> Unit,
    leaveGroup: () -> Unit
) {
    val conversationOptionSheetState = remember(conversationSheetContent) {
        ConversationOptionSheetState()
    }

    when (conversationOptionSheetState.currentNavigation) {
        ConversationOptionNavigation.Home -> {
            MenuModalSheetContent(
                headerTitle = conversationSheetContent.title,
                headerIcon = {
                    if (conversationSheetContent is ConversationSheetContent.GroupConversation) {
                        GroupConversationAvatar(colorValue = conversationSheetContent.groupColorValue)
                    } else if (conversationSheetContent is ConversationSheetContent.PrivateConversation) {
                        UserProfileAvatar(userAvatarAsset = conversationSheetContent.avatarAsset)
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
                            action = { NotificationsOptionsItemAction(mutedStatus) },
                            onItemClick = { conversationOptionSheetState.toNotification() }
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
                        if (conversationSheetContent is ConversationSheetContent.PrivateConversation) {
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
        ConversationOptionNavigation.Notification -> {
            Text("This is test for muting")
        }
    }

}

@Composable
fun NotificationsOptionsItemAction(
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

class ConversationOptionSheetState {

    var currentNavigation: ConversationOptionNavigation by mutableStateOf(ConversationOptionNavigation.Home)
        private set

    fun toNotification() {
        currentNavigation = ConversationOptionNavigation.Notification
    }

    fun toHome() {
        currentNavigation = ConversationOptionNavigation.Home
    }
}

sealed class ConversationOptionNavigation {
    object Home : ConversationOptionNavigation()
    object Notification : ConversationOptionNavigation()
}

sealed class ConversationSheetContent(val title: String, val conversationId: ConversationId?, var mutedStatus: MutedConversationStatus) {

    object Initial : ConversationSheetContent(
        title = "",
        conversationId = null,
        mutedStatus = MutedConversationStatus.AllAllowed
    )

    class PrivateConversation(
        title: String,
        val avatarAsset: UserAvatarAsset?,
        conversationId: ConversationId,
        mutedStatus: MutedConversationStatus
    ) : ConversationSheetContent(
        title = title,
        conversationId = conversationId,
        mutedStatus = mutedStatus
    )

    class GroupConversation(
        title: String,
        val groupColorValue: Long,
        conversationId: ConversationId,
        mutedStatus: MutedConversationStatus
    ) : ConversationSheetContent(
        title = title,
        conversationId = conversationId,
        mutedStatus = mutedStatus
    )
//
//    fun updateCurrentEditingMutedStatus(mutedStatus: MutedConversationStatus) {
//        this.mutedStatus = mutedStatus
//    }
}

//data class NotificationsOptionsItem(
//    val muteConversationAction: () -> Unit,
//    val mutedStatus: MutedConversationStatus
//)
