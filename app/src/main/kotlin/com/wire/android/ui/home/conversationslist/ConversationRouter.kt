package com.wire.android.ui.home.conversationslist

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.FloatingActionButton
import com.wire.android.ui.common.WireBottomTabItemData
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.home.conversationslist.ConversationOperationErrorState.MutingOperationErrorState
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationOptionNavigation
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheetContent
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalMaterialApi
// Since the HomeScreen is responsible for displaying the bottom sheet content,
// we create a bridge that passes the content of the BottomSheet
// also we expose the lambda which expands the BottomSheet from the HomeScreen
@Composable
fun ConversationRouterHomeBridge(
    viewModel: ConversationListViewModel,
    onHomeBottomSheetContentChanged: (@Composable ColumnScope.() -> Unit) -> Unit,
    onBottomSheetVisibilityChanged: () -> Unit,
    onScrollPositionProviderChanged: (() -> Int) -> Unit
) {

    fun openConversationBottomSheet(
        conversationItem: ConversationItem,
        conversationOptionNavigation: ConversationOptionNavigation = ConversationOptionNavigation.Home
    ) {
        onHomeBottomSheetContentChanged {
            val conversationState = rememberConversationSheetState(
                conversationItem = conversationItem,
                conversationOptionNavigation = conversationOptionNavigation
            )
            // if we reopen the BottomSheet of the previous conversation for example:
            // when the user swipes down the BottomSheet manually when having mute option open
            // we want to reopen it in the "home" section, but ONLY when the user reopens the BottomSheet
            // by holding the conversation item, not when the notification icon is pressed, therefore when
            // conversationOptionNavigation is equal to ConversationOptionNavigation.MutingNotificationOption
            conversationState.conversationId?.let { conversationId ->
                if (conversationId == conversationItem.conversationId &&
                    conversationOptionNavigation != ConversationOptionNavigation.MutingNotificationOption
                ) {
                    conversationState.toHome()
                }
            }

            ConversationSheetContent(
                conversationSheetState = conversationState,
                // FIXME: Compose - Find a way to not recreate this lambda
                onMutingConversationStatusChange = { mutedStatus ->
                    conversationState.muteConversation(mutedStatus)
                    viewModel.muteConversation(conversationId = conversationState.conversationId, mutedStatus)
                },
                addConversationToFavourites = viewModel::addConversationToFavourites,
                moveConversationToFolder = viewModel::moveConversationToFolder,
                moveConversationToArchive = viewModel::moveConversationToArchive,
                clearConversationContent = viewModel::clearConversationContent,
                blockUser = viewModel::blockUser,
                leaveGroup = viewModel::leaveGroup
            )
        }

        onBottomSheetVisibilityChanged()
    }

    ConversationRouter(
        uiState = viewModel.state,
        errorState = viewModel.errorState,
        openConversation = viewModel::openConversation,
        openNewConversation = viewModel::openNewConversation,
        onEditConversationItem = { conversationItem ->
            openConversationBottomSheet(
                conversationItem = conversationItem
            )
        },
        onScrollPositionProviderChanged = onScrollPositionProviderChanged,
        onError = onBottomSheetVisibilityChanged,
        openProfile = viewModel::openUserProfile,
        onEditNotifications = { conversationItem ->
            openConversationBottomSheet(
                conversationItem = conversationItem,
                conversationOptionNavigation = ConversationOptionNavigation.MutingNotificationOption
            )
        },
        onJoinCall = viewModel::joinOngoingCall
    )
}

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalMaterialApi
@Composable
private fun ConversationRouter(
    uiState: ConversationListState,
    errorState: ConversationOperationErrorState?,
    openConversation: (ConversationId) -> Unit,
    openNewConversation: () -> Unit,
    onEditConversationItem: (ConversationItem) -> Unit,
    onEditNotifications: (ConversationItem) -> Unit,
    onJoinCall: (ConversationId) -> Unit,
    onScrollPositionProviderChanged: (() -> Int) -> Unit,
    onError: () -> Unit,
    openProfile: (UserId) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val items = ConversationsItem.values().toList()
    var currentItemIndex: Int by remember { mutableStateOf(0) }

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
            // TODO uncomment when CallsScreen and MentionScreen will be implemented
//            WireBottomTabBar(bottomBarItems(items, uiState), currentItemIndex) { currentItemIndex = it }
        }
    ) { internalPadding ->
        Column(modifier = Modifier.padding(internalPadding)) {
            Crossfade(targetState = currentItemIndex) {
                with(uiState) {
                    when (items[it]) {
                        ConversationsItem.All ->
                            AllConversationScreen(
                                newActivities = newActivities,
                                conversations = conversations,
                                onOpenConversation = openConversation,
                                onEditConversation = onEditConversationItem,
                                onScrollPositionProviderChanged = onScrollPositionProviderChanged,
                                onOpenUserProfile = openProfile,
                                onOpenConversationNotificationsSettings = onEditNotifications,
                                onJoinCall = onJoinCall
                            )
                        ConversationsItem.Calls ->
                            CallsScreen(
                                missedCalls = missedCalls,
                                callHistory = callHistory,
                                onCallItemClick = openConversation,
                                onEditConversationItem = onEditConversationItem,
                                onScrollPositionProviderChanged = onScrollPositionProviderChanged,
                                onOpenUserProfile = openProfile,
                                openConversationNotificationsSettings = onEditNotifications,
                                onJoinCall = onJoinCall
                            )
                        ConversationsItem.Mentions ->
                            MentionScreen(
                                unreadMentions = unreadMentions,
                                allMentions = allMentions,
                                onMentionItemClick = openConversation,
                                onEditConversationItem = onEditConversationItem,
                                onScrollPositionProviderChanged = onScrollPositionProviderChanged,
                                onOpenUserProfile = openProfile,
                                openConversationNotificationsSettings = onEditNotifications,
                                onJoinCall = onJoinCall
                            )
                    }
                }
            }
        }
    }
}

private fun bottomBarItems(
    items: List<ConversationsItem>,
    uiListState: ConversationListState
): List<WireBottomTabItemData> {
    return items.map { conversationsNavigationItem ->
        when (conversationsNavigationItem) {
            ConversationsItem.All -> conversationsNavigationItem.toBottomNavigationItemData(uiListState.conversations.size)
            ConversationsItem.Calls -> conversationsNavigationItem.toBottomNavigationItemData(uiListState.missedCallsCount)
            ConversationsItem.Mentions -> conversationsNavigationItem.toBottomNavigationItemData(uiListState.unreadMentionsCount)
        }
    }
}
