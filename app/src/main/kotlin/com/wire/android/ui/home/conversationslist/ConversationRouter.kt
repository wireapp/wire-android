package com.wire.android.ui.home.conversationslist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.wire.android.ui.home.conversationslist.model.ConversationType
import com.wire.android.ui.main.conversationlist.navigation.ConversationsNavigationItem
import com.wire.kalium.logic.data.id.ConversationId

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalMaterialApi
// Since the HomeScreen is responsible for displaying the bottom sheet content,
// we create a bridge that passes the content of the bottomsheet
// also we expose the lambda which expands the bottomsheet from the homescreen
@Composable
fun ConversationRouterHomeBridge(
    onHomeBottomSheetContentChange: (@Composable ColumnScope.() -> Unit) -> Unit,
    onExpandHomeBottomSheet: () -> Unit,
    onScrollPositionChanged: (Int) -> Unit
) {
    val conversationState = rememberConversationState()
    val viewModel: ConversationListViewModel = hiltViewModel()

    // we want to relaunch the onHomeBottomSheetContentChange lambda each time the content changes
    // to pass the new Composable
    LaunchedEffect(conversationState.modalBottomSheetContentState) {
        onHomeBottomSheetContentChange {
            ConversationSheetContent(
                modalBottomSheetContentState = conversationState.modalBottomSheetContentState.value,
                muteConversation = { viewModel.muteConversation("someId") },
                addConversationToFavourites = { viewModel.addConversationToFavourites("someId") },
                moveConversationToFolder = { viewModel.moveConversationToFolder("someId") },
                moveConversationToArchive = { viewModel.moveConversationToArchive("someId") },
                clearConversationContent = { viewModel.clearConversationContent("someId") },
                blockUser = { viewModel.blockUser("someId") },
                leaveGroup = { viewModel.leaveGroup("someId") }
            )
        }
    }

    ConversationRouter(
        uiState = viewModel.state,
        conversationState = conversationState,
        openConversation = { viewModel.openConversation(it) },
        openNewConversation = { viewModel.openNewConversation() },
        onExpandBottomSheet = { onExpandHomeBottomSheet() },
        onScrollPositionChanged = onScrollPositionChanged
    )
}

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalMaterialApi
@Composable
private fun ConversationRouter(
    uiState: ConversationListState,
    conversationState: ConversationState,
    openConversation: (ConversationId) -> Unit,
    openNewConversation: () -> Unit,
    onExpandBottomSheet: () -> Unit,
    onScrollPositionChanged: (Int) -> Unit,
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
                onClick = openNewConversation
            )
        },
        bottomBar = { WireBottomNavigationBar(ConversationNavigationItems(uiState), conversationState.navHostController) }
    ) {

        fun editConversation(conversationType: ConversationType) {
            conversationState.changeModalSheetContentState(conversationType)
            onExpandBottomSheet()
        }

        with(uiState) {
            // Change to a AnimatedNavHost and composable from accompanist lib to add transitions animations
            NavHost(conversationState.navHostController, startDestination = ConversationsNavigationItem.All.route) {
                composable(
                    route = ConversationsNavigationItem.All.route,
                    content = {
                        AllConversationScreen(
                            newActivities = newActivities,
                            conversations = conversations,
                            onOpenConversationClick = openConversation,
                            onEditConversationItem = ::editConversation,
                            onScrollPositionChanged = onScrollPositionChanged
                        )
                    }
                )
                composable(
                    route = ConversationsNavigationItem.Calls.route,
                    content = {
                        CallScreen(
                            missedCalls = missedCalls,
                            callHistory = callHistory,
                            onCallItemClick = openConversation,
                            onEditConversationItem = ::editConversation,
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
                            onEditConversationItem = ::editConversation,
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
            ConversationsNavigationItem.All -> conversationsNavigationItem.toBottomNavigationItemData(uiListState.newActivityCount)
            ConversationsNavigationItem.Calls -> conversationsNavigationItem.toBottomNavigationItemData(uiListState.missedCallsCount)
            ConversationsNavigationItem.Mentions -> conversationsNavigationItem.toBottomNavigationItemData(uiListState.unreadMentionsCount)
        }
    }
}
