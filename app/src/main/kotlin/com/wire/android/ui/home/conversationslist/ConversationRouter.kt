package com.wire.android.ui.home.conversationslist

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.wire.android.R
import com.wire.android.ui.common.FloatingActionButton
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.WireBottomNavigationBar
import com.wire.android.ui.common.WireBottomNavigationItemData
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemIcon
import com.wire.android.ui.common.bottomsheet.ModalSheetHeaderItem
import com.wire.android.ui.common.bottomsheet.buildMenuSheetItems
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.LocalHomeState
import com.wire.android.ui.home.conversations.common.GroupConversationAvatar
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheet
import com.wire.android.ui.home.conversationslist.bottomsheet.ModalSheetContent
import com.wire.android.ui.main.conversationlist.navigation.ConversationsNavigationItem
import com.wire.kalium.logic.data.conversation.ConversationId

@ExperimentalMaterial3Api
@ExperimentalMaterialApi
@Composable
fun ConversationRouter(viewModel: ConversationListViewModel = hiltViewModel()) {
    val uiState = viewModel.state

    ConversationRouter(
        uiState = uiState,
        conversationState = rememberConversationState(),
        openConversation = { id -> viewModel.openConversation(id) },
        muteConversation = { id -> viewModel.muteConversation(id) },
        addConversationToFavourites = { id -> viewModel.addConversationToFavourites(id) },
        moveConversationToFolder = { id -> viewModel.moveConversationToFolder(id) },
        moveConversationToArchive = { id -> viewModel.moveConversationToArchive(id) },
        clearConversationContent = { id -> viewModel.clearConversationContent(id) },
        blockUser = { id -> viewModel.blockUser(id) },
        leaveGroup = { id -> viewModel.leaveGroup(id) },
        updateScrollPosition = { position -> viewModel.updateScrollPosition(position) }
    )
}

@ExperimentalMaterial3Api
@ExperimentalMaterialApi
@Composable
private fun ConversationRouter(
    uiState: ConversationListState,
    conversationState: ConversationState,
    openConversation: (ConversationId) -> Unit,
    muteConversation: (String) -> Unit,
    addConversationToFavourites: (String) -> Unit,
    moveConversationToFolder: (String) -> Unit,
    moveConversationToArchive: (String) -> Unit,
    clearConversationContent: (String) -> Unit,
    blockUser: (String) -> Unit,
    leaveGroup: (String) -> Unit,
    updateScrollPosition: (Int) -> Unit,
) {
    val homeState = LocalHomeState.current

    val state = conversationState.modalBottomSheetContentState.value

    homeState!!.changeBottomSheetContent {
        ModalSheetHeaderItem(
            title = state.title,
            leadingIcon = {
                if (state is ModalSheetContent.GroupConversationEdit) {
                    GroupConversationAvatar(colorValue = state.groupColorValue)
                } else {
                    UserProfileAvatar()
                }
            }
        )

        val items: List<@Composable () -> Unit> = listOf(
            {
                MenuBottomSheetItem(
                    title = stringResource(R.string.label_mute),
                    icon = {
                        MenuItemIcon(
                            id = R.drawable.ic_mute,
                            contentDescription = stringResource(R.string.content_description_mute),
                        )
                    },
                    onItemClick = { }
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
                    onItemClick = { }
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
                    onItemClick = { }
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
                    onItemClick = { }
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
                    onItemClick = { }
                )
            },
            {
                if (state is ModalSheetContent.PrivateConversationEdit) {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) {
                        MenuBottomSheetItem(
                            icon = {
                                MenuItemIcon(
                                    id = R.drawable.ic_block,
                                    contentDescription = stringResource(R.string.content_description_block_the_user),
                                )
                            },
                            title = stringResource(R.string.label_block),
                            onItemClick = { }
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
                            onItemClick = { }
                        )
                    }
                }
            }
        )

        buildMenuSheetItems(items = items)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                text = stringResource(R.string.label_new),
                icon = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_conversation),
                        contentDescription = stringResource(R.string.content_description_new_conversation),
                        contentScale = ContentScale.FillBounds,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
                        modifier = Modifier
                            .padding(start = dimensions().spacing4x, top = dimensions().spacing2x)
                            .size(dimensions().fabIconSize)
                    )
                },
                onClick = {}
            )
        },
        bottomBar = { WireBottomNavigationBar(ConversationNavigationItems(uiState), conversationState.navHostController) }
    ) {
        with(uiState) {
            NavHost(conversationState.navHostController, startDestination = ConversationsNavigationItem.All.route) {
                composable(
                    route = ConversationsNavigationItem.All.route,
                    content = {
                        AllConversationScreen(
                            newActivities = newActivities,
                            conversations = conversations,
                            onOpenConversationClick = openConversation,
                            onEditConversationItem = { homeState.expandBottomSheet() },
                            onScrollPositionChanged = updateScrollPosition
                        )
                    })
                composable(
                    route = ConversationsNavigationItem.Calls.route,
                    content = {
                        CallScreen(
                            missedCalls = missedCalls,
                            callHistory = callHistory,
                            onCallItemClick = openConversation,
                            onEditConversationItem = conversationState::showModalSheet,
                            onScrollPositionChanged = updateScrollPosition
                        )
                    })
                composable(
                    route = ConversationsNavigationItem.Mentions.route,
                    content = {
                        MentionScreen(
                            unreadMentions = unreadMentions,
                            allMentions = allMentions,
                            onMentionItemClick = openConversation,
                            onEditConversationItem = conversationState::showModalSheet,
                            onScrollPositionChanged = updateScrollPosition
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ConversationNavigationItems(
    uiListState: ConversationListState
): List<WireBottomNavigationItemData> {
    return ConversationsNavigationItem.values().map { conversationsNavigationItem ->
        when (conversationsNavigationItem) {
            ConversationsNavigationItem.All -> conversationsNavigationItem.toBottomNavigationItemData(uiListState.newActivityCount)
            ConversationsNavigationItem.Calls -> conversationsNavigationItem.toBottomNavigationItemData(uiListState.missedCallsCount)
            ConversationsNavigationItem.Mentions -> conversationsNavigationItem.toBottomNavigationItemData(uiListState.unreadMentionsCount)
        }
    }
}
