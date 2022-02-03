package com.wire.android.ui.home.conversationslist

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.wire.android.R
import com.wire.android.ui.common.FloatingActionButton
import com.wire.android.ui.common.WireBottomNavigationBar
import com.wire.android.ui.common.WireBottomNavigationItemData
import com.wire.android.ui.main.conversationlist.navigation.ConversationsNavigationItem


@ExperimentalMaterial3Api
@ExperimentalMaterialApi
@Composable
fun ConversationRouter(conversationListViewModel: ConversationListViewModel = hiltViewModel()) {
    val uiState by conversationListViewModel.state.collectAsState()

    ConversationRouter(
        uiState = uiState,
        conversationState = rememberConversationState(),
        openConversation = { id -> conversationListViewModel.openConversation(id) },
        muteConversation = { id -> conversationListViewModel.muteConversation(id) },
        addConversationToFavourites = { id -> conversationListViewModel.addConversationToFavourites(id) },
        moveConversationToFolder = { id -> conversationListViewModel.moveConversationToFolder(id) },
        moveConversationToArchive = { id -> conversationListViewModel.moveConversationToArchive(id) },
        clearConversationContent = { id -> conversationListViewModel.clearConversationContent(id) },
        blockUser = { id -> conversationListViewModel.blockUser(id) },
        leaveGroup = { id -> conversationListViewModel.leaveGroup(id) },
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
    ModalBottomSheetLayout(
        sheetState = conversationState.modalBottomSheetState,
        //TODO: create a shape object inside the materialtheme 3 component
        sheetShape = MaterialTheme.shapes.large.copy(topStart = CornerSize(12.dp), topEnd = CornerSize(12.dp)),
        sheetContent = {
            when (val contentType = conversationState.modalBottomSheetContentState.value) {
                is ModalSheetContent.GroupConversationEdit -> GroupConversationSheet(
                    groupConversationEdit = contentType,
                    onMuteClick = { muteConversation("someId") },
                    onAddToFavouritesClick = { addConversationToFavourites("someId") },
                    onMoveToFolderClick = { moveConversationToFolder("someId") },
                    onMoveToArchiveClick = { moveConversationToArchive("someId") },
                    onClearContentClick = { clearConversationContent("someId") },
                    onLeaveClick = { leaveGroup("someId") })
                is ModalSheetContent.PrivateConversationEdit -> PrivateConversationSheet(
                    content = contentType,
                    onMuteClick = { muteConversation("someId") },
                    onAddToFavouritesClick = { addConversationToFavourites("someId") },
                    onMoveToFolderClick = { moveConversationToFolder("someId") },
                    onMoveToArchiveClick = { moveConversationToArchive("someId") },
                    onClearContentClick = { clearConversationContent("someId") },
                    onBlockClick = { blockUser("someId") }
                )
                ModalSheetContent.Initial -> CircularProgressIndicator() //TODO: add loading state here
            }
        }
    ) {
        Scaffold(
            floatingActionButton = { FloatingActionButton(stringResource(R.string.label_new), {}) },
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


