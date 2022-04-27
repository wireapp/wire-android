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
import com.wire.android.R
import com.wire.android.ui.common.FloatingActionButton
import com.wire.android.ui.common.WireBottomNavigationBar
import com.wire.android.ui.common.WireBottomNavigationItemData
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.home.conversationslist.ConversationOperationErrorState.MutingOperationErrorState
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheetContent
import com.wire.android.ui.home.conversationslist.bottomsheet.NotificationsOptionsItem
import com.wire.android.ui.home.conversationslist.model.ConversationType
import com.wire.android.ui.home.conversationslist.navigation.ConversationsNavigationItem
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
    onBottomSheetVisibilityToggled: () -> Unit,
    onScrollPositionChanged: (Int) -> Unit
) {
    val conversationState = rememberConversationState()
    val viewModel: ConversationListViewModel = hiltViewModel()
    val mutingConversationState = rememberMutingConversationState(conversationState.modalBottomSheetContentState.value.mutedStatus)

    // we want to relaunch the onHomeBottomSheetContentChange lambda each time the content changes
    // to pass the new Composable
    LaunchedEffect(conversationState.modalBottomSheetContentState) {
        onHomeBottomSheetContentChange {
            when (conversationState.isEditingMutedSetting.value) {
                true -> {
                    MutingOptionsSheetContent(
                        mutingConversationState = mutingConversationState,
                        onItemClick = { conversationId, mutedStatus ->
                            viewModel.muteConversation(conversationId, mutedStatus)
                            conversationState.modalBottomSheetContentState.value.updateCurrentEditingMutedStatus(mutedStatus)
                        },
                        onBackClick = {
                            mutingConversationState.closeMutedStatusSheetContent()
                            conversationState.modalBottomSheetContentState.value.updateCurrentEditingMutedStatus(
                                mutingConversationState.mutedStatus
                            )
                            conversationState.toggleEditMutedSetting(false)
                        }
                    )
                }
                false -> {
                    ConversationSheetContent(
                        modalBottomSheetContentState = conversationState.modalBottomSheetContentState.value,
                        notificationsOptionsItem = NotificationsOptionsItem(
                            muteConversationAction = {
                                mutingConversationState.openMutedStatusSheetContent(
                                    conversationState.modalBottomSheetContentState.value.conversationId,
                                    conversationState.modalBottomSheetContentState.value.mutedStatus
                                )
                                // here we trigger a sheet content change, enabling muted settings toggle
                                conversationState.toggleEditMutedSetting(true)
                            },
                            mutedStatus = mutingConversationState.mutedStatus
                        ),
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
    }

    ConversationRouter(
        uiState = viewModel.state,
        errorState = viewModel.errorState,
        conversationState = conversationState,
        openConversation = { viewModel.openConversation(it) },
        openNewConversation = { viewModel.openNewConversation() },
        onExpandBottomSheet = { onBottomSheetVisibilityToggled() },
        onScrollPositionChanged = onScrollPositionChanged,
        onError = onBottomSheetVisibilityToggled
    )
}

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalMaterialApi
@Composable
private fun ConversationRouter(
    uiState: ConversationListState,
    errorState: ConversationOperationErrorState?,
    conversationState: ConversationState,
    openConversation: (ConversationId) -> Unit,
    openNewConversation: () -> Unit,
    onExpandBottomSheet: (ConversationId) -> Unit,
    onScrollPositionChanged: (Int) -> Unit,
    onError: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

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
        bottomBar = { WireBottomNavigationBar(ConversationNavigationItems(uiState), conversationState.navHostController) }
    ) {

        fun editConversation(conversationType: ConversationType) {
            conversationState.changeModalSheetContentState(conversationType)
            onExpandBottomSheet(conversationType.conversationId)
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
                        CallsScreen(
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
