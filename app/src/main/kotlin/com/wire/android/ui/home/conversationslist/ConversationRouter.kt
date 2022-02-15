package com.wire.android.ui.home.conversationslist

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheet
import com.wire.android.ui.main.conversationlist.navigation.ConversationsNavigationItem

@ExperimentalMaterial3Api
@ExperimentalMaterialApi
@Composable
fun ConversationRouter(viewModel: ConversationListViewModel = hiltViewModel()) {
    val uiState by viewModel.state.collectAsState()

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
    )
}

@ExperimentalMaterial3Api
@ExperimentalMaterialApi
@Composable
private fun ConversationRouter(
    uiState: ConversationListState,
    conversationState: ConversationState,
    openConversation: (String) -> Unit,
    muteConversation: (String) -> Unit,
    addConversationToFavourites: (String) -> Unit,
    moveConversationToFolder: (String) -> Unit,
    moveConversationToArchive: (String) -> Unit,
    clearConversationContent: (String) -> Unit,
    blockUser: (String) -> Unit,
    leaveGroup: (String) -> Unit
) {
    ConversationSheet(
        sheetState = conversationState.modalBottomSheetState,
        modalBottomSheetContentState = conversationState.modalBottomSheetContentState.value,
        muteConversation = muteConversation,
        addConversationToFavourites = addConversationToFavourites,
        moveConversationToFolder = moveConversationToFolder,
        moveConversationToArchive = moveConversationToArchive,
        clearConversationContent = clearConversationContent,
        blockUser = blockUser,
        leaveGroup = leaveGroup
    ) {
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
                                onEditConversationItem = conversationState::showModalSheet
                            )
                        })
                    composable(
                        route = ConversationsNavigationItem.Calls.route,
                        content = {
                            CallScreen(
                                missedCalls = missedCalls,
                                callHistory = callHistory,
                                onCallItemClick = openConversation,
                                onEditConversationItem = conversationState::showModalSheet
                            )
                        })
                    composable(
                        route = ConversationsNavigationItem.Mentions.route,
                        content = {
                            MentionScreen(
                                unreadMentions = unreadMentions,
                                allMentions = allMentions,
                                onMentionItemClick = openConversation,
                                onEditConversationItem = conversationState::showModalSheet
                            )
                        }
                    )
                }
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


