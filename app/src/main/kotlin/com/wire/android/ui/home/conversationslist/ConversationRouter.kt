package com.wire.android.ui.home.conversationslist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wire.android.R
import com.wire.android.ui.common.FloatingActionButton
import com.wire.android.ui.common.WireBottomNavigationBar
import com.wire.android.ui.common.WireBottomNavigationItemData
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.home.conversationslist.ConversationOperationErrorState.MutingOperationErrorState
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheetContent
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.navigation.ConversationsNavigationItem

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalMaterialApi
// Since the HomeScreen is responsible for displaying the bottom sheet content,
// we create a bridge that passes the content of the bottomsheet
// also we expose the lambda which expands the bottomsheet from the homescreen
@Composable
fun ConversationRouterHomeBridge(
    onHomeBottomSheetContentChange: (@Composable ColumnScope.() -> Unit) -> Unit,
    onBottomSheetVisibilityChange: () -> Unit,
    onScrollPositionChanged: (Int) -> Unit
) {
    val viewModel: ConversationListViewModel = hiltViewModel()
    val conversationState: ConversationState = remember {
        ConversationState()
    }

    // we want to relaunch the onHomeBottomSheetContentChange lambda each time the content changes
    // to pass the new Composable
    LaunchedEffect(conversationState.modalBottomSheetContentState) {
        conversationState.modalBottomSheetContentState?.let { conversationSheetContent ->
            onHomeBottomSheetContentChange {
                ConversationSheetContent(
                    conversationSheetContent = conversationSheetContent,
                    onMutingConversationStatusChange = conversationState::muteConversation,
                    addConversationToFavourites = { viewModel.addConversationToFavourites("someId") },
                    moveConversationToFolder = { viewModel.moveConversationToFolder("someId") },
                    moveConversationToArchive = { viewModel.moveConversationToArchive("someId") },
                    clearConversationContent = { viewModel.clearConversationContent("someId") },
                    blockUser = { viewModel.blockUser("someId") },
                    leaveGroup = { viewModel.leaveGroup("someId") }
                )
            }
        }
    }

    ConversationRouter(
        uiState = viewModel.state,
        errorState = viewModel.errorState,
        openConversation = viewModel::openConversation,
        openNewConversation = viewModel::openNewConversation,
        onEditConversationItem = { conversationItem ->
            conversationState.changeModalSheetContentState(conversationItem.conversationType)
            onBottomSheetVisibilityChange()
        },
        onScrollPositionChanged = onScrollPositionChanged,
        onError = onBottomSheetVisibilityChange
    )
}

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalMaterialApi
@Composable
private fun ConversationRouter(
    uiState: ConversationListState,
    errorState: ConversationOperationErrorState?,
    openConversation: (ConversationItem) -> Unit,
    openNewConversation: () -> Unit,
    onEditConversationItem: (ConversationItem) -> Unit,
    onScrollPositionChanged: (Int) -> Unit,
    onError: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val navHostController = rememberNavController()

    errorState?.let { errorType ->
        val message = when (errorType) {
            is MutingOperationErrorState -> stringResource(id = R.string.error_updating_muting_setting)
        }
        LaunchedEffect(errorType) {
            onError()
            snackbarHostState.showSnackbar(message)
        }
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
                onClick = openNewConversation
            )
        },
        snackbarHost = {
            SwipeDismissSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.fillMaxWidth()
            )
        },
        bottomBar = {
            WireBottomNavigationBar(ConversationNavigationItems(uiState), navHostController)
        }
    ) { internalPadding ->

        with(uiState) {
            // Change to a AnimatedNavHost and composable from accompanist lib to add transitions animations
            NavHost(
                navHostController,
                startDestination = ConversationsNavigationItem.All.route,
                modifier = Modifier.padding(internalPadding)
            ) {
                composable(
                    route = ConversationsNavigationItem.All.route,
                    content = {
                        AllConversationScreen(
                            newActivities = newActivities,
                            conversations = conversations,
                            onOpenConversationClick = openConversation,
                            onEditConversationItem = onEditConversationItem,
                            onScrollPositionChanged = onScrollPositionChanged
                        )
                    }
                )
                composable(
                    route = ConversationsNavigationItem.Calls.route,
                    content = {
                        CallsScreen(
                            missedCalls = missedCalls,
                            callHistory = callHistory,
                            onCallItemClick = openConversation,
                            onEditConversationItem = onEditConversationItem,
                            onScrollPositionChanged = onScrollPositionChanged
                        )
                    }
                )
                composable(
                    route = ConversationsNavigationItem.Mentions.route,
                    content = {
                        MentionScreen(
                            unreadMentions = unreadMentions,
                            allMentions = allMentions,
                            onMentionItemClick = openConversation,
                            onEditConversationItem = onEditConversationItem,
                            onScrollPositionChanged = onScrollPositionChanged
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
            ConversationsNavigationItem.All -> conversationsNavigationItem.toBottomNavigationItemData(uiListState.conversations.size)
            ConversationsNavigationItem.Calls -> conversationsNavigationItem.toBottomNavigationItemData(uiListState.missedCallsCount)
            ConversationsNavigationItem.Mentions -> conversationsNavigationItem.toBottomNavigationItemData(uiListState.unreadMentionsCount)
        }
    }
}
