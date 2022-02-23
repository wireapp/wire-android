package com.wire.android.ui.home.conversationslist

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
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
import com.wire.android.ui.common.WireBottomNavigationBar
import com.wire.android.ui.common.WireBottomNavigationItemData
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheetContent
import com.wire.android.ui.main.conversationlist.navigation.ConversationsNavigationItem
import com.wire.kalium.logic.data.conversation.ConversationId

@ExperimentalMaterial3Api
@ExperimentalMaterialApi
@Composable
fun ConversationRouterHomeBridge(
    onHomeBottomSheetContent: (@Composable ColumnScope.() -> Unit) -> Unit,
    onExpandHomeBottomSheet: () -> Unit
) {
    val conversationState = rememberConversationState()
    val conversationListViewModel: ConversationListViewModel = hiltViewModel()

    onHomeBottomSheetContent {
        ConversationSheetContent(
            modalBottomSheetContentState = conversationState.modalBottomSheetContentState.value,
            muteConversation = { conversationListViewModel.muteConversation("someId") },
            addConversationToFavourites = { conversationListViewModel.addConversationToFavourites("someId") },
            moveConversationToFolder = { conversationListViewModel.moveConversationToFolder("someId") },
            moveConversationToArchive = { conversationListViewModel.moveConversationToArchive("someId") },
            clearConversationContent = { conversationListViewModel.clearConversationContent("someId") },
            blockUser = { conversationListViewModel.blockUser("someId") },
            leaveGroup = { conversationListViewModel.leaveGroup("someId") }
        )
    }
}

@ExperimentalMaterial3Api
@ExperimentalMaterialApi
@Composable
private fun ConversationRouterHomeBridge(
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
    val state = conversationState.modalBottomSheetContentState.value


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
                            onEditConversationItem = { },
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
