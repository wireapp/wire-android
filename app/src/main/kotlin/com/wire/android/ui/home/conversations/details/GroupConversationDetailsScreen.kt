/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 *
 */

package com.wire.android.ui.home.conversations.details

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.ramcosta.composedestinations.result.ResultRecipient
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.CollapsingTopBarScaffold
import com.wire.android.ui.common.MoreOptionIcon
import com.wire.android.ui.common.TabItem
import com.wire.android.ui.common.WireTabRow
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.conversation.ConversationSheetContent
import com.wire.android.ui.common.bottomsheet.conversation.rememberConversationSheetState
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.calculateCurrentTab
import com.wire.android.ui.common.dialogs.ArchiveConversationDialog
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.topBarElevation
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.destinations.AddMembersSearchScreenDestination
import com.wire.android.ui.destinations.EditConversationNameScreenDestination
import com.wire.android.ui.destinations.EditGuestAccessScreenDestination
import com.wire.android.ui.destinations.EditSelfDeletingMessagesScreenDestination
import com.wire.android.ui.destinations.GroupConversationAllParticipantsScreenDestination
import com.wire.android.ui.destinations.OtherUserProfileScreenDestination
import com.wire.android.ui.destinations.SelfUserProfileScreenDestination
import com.wire.android.ui.destinations.ServiceDetailsScreenDestination
import com.wire.android.ui.home.conversations.details.dialog.ClearConversationContentDialog
import com.wire.android.ui.home.conversations.details.editguestaccess.EditGuestAccessParams
import com.wire.android.ui.home.conversations.details.menu.DeleteConversationGroupDialog
import com.wire.android.ui.home.conversations.details.menu.GroupConversationDetailsBottomSheetEventsHandler
import com.wire.android.ui.home.conversations.details.menu.LeaveConversationGroupDialog
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptions
import com.wire.android.ui.home.conversations.details.participants.GroupConversationParticipants
import com.wire.android.ui.home.conversations.details.participants.GroupConversationParticipantsState
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.ui.home.conversationslist.model.DialogState
import com.wire.android.ui.home.conversationslist.model.GroupDialogState
import com.wire.android.ui.destinations.SearchConversationMessagesScreenDestination
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.ui.UIText
import kotlinx.coroutines.launch

@RootNavGraph
@Destination(
    navArgsDelegate = GroupConversationDetailsNavArgs::class,
    style = PopUpNavigationAnimation::class,
)
@Composable
fun GroupConversationDetailsScreen(
    navigator: Navigator,
    viewModel: GroupConversationDetailsViewModel = hiltViewModel(),
    resultNavigator: ResultBackNavigator<GroupConversationDetailsNavBackArgs>,
    groupConversationDetailResultRecipient: ResultRecipient<EditConversationNameScreenDestination, Boolean>,
) {
    val scope = rememberCoroutineScope()
    val resources = LocalContext.current.resources
    val snackbarHostState = LocalSnackbarHostState.current
    val showSnackbarMessage: (UIText) -> Unit = remember { { scope.launch { snackbarHostState.showSnackbar(it.asString(resources)) } } }

    val onSearchConversationMessagesClick: () -> Unit = {
        navigator.navigate(
            NavigationCommand(
                SearchConversationMessagesScreenDestination(
                    conversationId = viewModel.conversationId
                )
            )
        )
    }

    GroupConversationDetailsContent(
        conversationSheetContent = viewModel.conversationSheetContent,
        bottomSheetEventsHandler = viewModel,
        onBackPressed = navigator::navigateBack,
        openFullListPressed = {
            navigator.navigate(NavigationCommand(GroupConversationAllParticipantsScreenDestination(viewModel.conversationId)))
        },
        onProfilePressed = { participant ->
            when {
                participant.isSelf -> navigator.navigate(NavigationCommand(SelfUserProfileScreenDestination))
                participant.isService && participant.botService != null ->
                    navigator.navigate(NavigationCommand(ServiceDetailsScreenDestination(participant.botService, viewModel.conversationId)))

                else -> navigator.navigate(NavigationCommand(OtherUserProfileScreenDestination(participant.id, viewModel.conversationId)))
            }
        },
        onAddParticipantsPressed = {
            navigator.navigate(
                NavigationCommand(
                    AddMembersSearchScreenDestination(
                        viewModel.conversationId,
                        viewModel.groupOptionsState.value.isServicesAllowed
                    )
                )
            )
        },
        groupParticipantsState = viewModel.groupParticipantsState,
        onLeaveGroup = {
            viewModel.leaveGroup(
                it,
                onSuccess = {
                    resultNavigator.setResult(
                        GroupConversationDetailsNavBackArgs(
                            groupConversationActionType = GroupConversationActionType.LEAVE_GROUP,
                            hasLeftGroup = true,
                            conversationName = it.conversationName
                        )
                    )
                    resultNavigator.navigateBack()
                },
                onFailure = showSnackbarMessage
            )
        },
        onDeleteGroup = {
            viewModel.deleteGroup(
                it,
                onSuccess = {
                    resultNavigator.setResult(
                        GroupConversationDetailsNavBackArgs(
                            groupConversationActionType = GroupConversationActionType.DELETE_GROUP,
                            isGroupDeleted = true,
                            conversationName = it.conversationName
                        )
                    )
                    resultNavigator.navigateBack()
                },
                onFailure = showSnackbarMessage
            )
        },
        onEditGuestAccess = {
            navigator.navigate(
                NavigationCommand(
                    EditGuestAccessScreenDestination(
                        viewModel.conversationId,
                        EditGuestAccessParams(
                            viewModel.groupOptionsState.value.isGuestAllowed,
                            viewModel.groupOptionsState.value.isServicesAllowed,
                            viewModel.groupOptionsState.value.isUpdatingGuestAllowed
                        )
                    )
                )
            )
        },
        onEditSelfDeletingMessages = {
            navigator.navigate(NavigationCommand(EditSelfDeletingMessagesScreenDestination(viewModel.conversationId)))
        },
        onEditGroupName = {
            navigator.navigate(NavigationCommand(EditConversationNameScreenDestination(viewModel.conversationId)))
        },
        isLoading = viewModel.requestInProgress,
        onSearchConversationMessagesClick = onSearchConversationMessagesClick
    )

    val tryAgainSnackBarMessage = stringResource(id = R.string.error_unknown_message)
    val successSnackBarMessage = stringResource(id = R.string.conversation_options_renamed)

    groupConversationDetailResultRecipient.onNavResult { result ->
        when (result) {
            is NavResult.Canceled -> {
                appLogger.i("Error with receiving navigation back args from editGroupName in GroupConversationDetailsScreen")
            }

            is NavResult.Value -> {
                scope.launch {
                    if (result.value) {
                        snackbarHostState.showSnackbar(successSnackBarMessage)
                    } else {
                        snackbarHostState.showSnackbar(tryAgainSnackBarMessage)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun GroupConversationDetailsContent(
    conversationSheetContent: ConversationSheetContent?,
    bottomSheetEventsHandler: GroupConversationDetailsBottomSheetEventsHandler,
    onBackPressed: () -> Unit,
    openFullListPressed: () -> Unit,
    onProfilePressed: (UIParticipant) -> Unit,
    onAddParticipantsPressed: () -> Unit,
    onEditGuestAccess: () -> Unit,
    onEditSelfDeletingMessages: () -> Unit,
    onEditGroupName: () -> Unit,
    onLeaveGroup: (GroupDialogState) -> Unit,
    onDeleteGroup: (GroupDialogState) -> Unit,
    groupParticipantsState: GroupConversationParticipantsState,
    isLoading: Boolean,
    onSearchConversationMessagesClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val resources = LocalContext.current.resources
    val snackbarHostState = LocalSnackbarHostState.current
    val lazyListStates: List<LazyListState> = GroupConversationDetailsTabItem.entries.map { rememberLazyListState() }
    val initialPageIndex = GroupConversationDetailsTabItem.OPTIONS.ordinal
    val pagerState = rememberPagerState(initialPage = initialPageIndex, pageCount = { GroupConversationDetailsTabItem.entries.size })
    val maxAppBarElevation = MaterialTheme.wireDimensions.topBarShadowElevation
    val currentTabState by remember { derivedStateOf { pagerState.calculateCurrentTab() } }
    val elevationState by remember { derivedStateOf { lazyListStates[currentTabState].topBarElevation(maxAppBarElevation) } }

    val conversationSheetState = rememberConversationSheetState(conversationSheetContent)

    val sheetState = rememberWireModalSheetState()
    val openBottomSheet: () -> Unit = remember { { scope.launch { sheetState.show() } } }
    val closeBottomSheetAndShowSnackbarMessage: (UIText) -> Unit = remember {
        {
            scope.launch {
                sheetState.hide()
                snackbarHostState.showSnackbar(it.asString(resources))
            }
        }
    }
    val getBottomSheetVisibility: () -> Boolean = remember(sheetState) { { sheetState.isVisible } }

    val deleteGroupDialogState = rememberVisibilityState<GroupDialogState>()
    val leaveGroupDialogState = rememberVisibilityState<GroupDialogState>()
    val clearConversationDialogState = rememberVisibilityState<DialogState>()
    val archiveConversationDialogState = rememberVisibilityState<DialogState>()

    LaunchedEffect(conversationSheetState.conversationSheetContent) {
        // on each closing BottomSheet we revert BSContent to Home.
        // So in case if user opened BS, went to MuteStatus BS and closed it by clicking outside of BS,
        // then opens BS again - Home BS suppose to be opened, not MuteStatus BS
        snapshotFlow { sheetState.isVisible }.collect { isVisible ->
            if (!isVisible) conversationSheetState.toHome()
        }
    }

    if (!isLoading) {
        deleteGroupDialogState.dismiss()
        leaveGroupDialogState.dismiss()
        clearConversationDialogState.dismiss()
        archiveConversationDialogState.dismiss()
    }

    CollapsingTopBarScaffold(
        topBarHeader = {
            WireCenterAlignedTopAppBar(
                elevation = elevationState,
                title = stringResource(R.string.conversation_details_title),
                navigationIconType = NavigationIconType.Close,
                onNavigationPressed = onBackPressed,
                actions = { MoreOptionIcon(onButtonClicked = openBottomSheet) }
            )
        },
        topBarCollapsing = {
            conversationSheetState.conversationSheetContent?.let {
                GroupConversationDetailsTopBarCollapsing(
                    title = it.title,
                    conversationId = it.conversationId,
                    totalParticipants = groupParticipantsState.data.allCount,
                    isLoading = isLoading,
                    onSearchConversationMessagesClick = onSearchConversationMessagesClick
                )
            }
        },
        topBarFooter = {
            AnimatedVisibility(
                visible = conversationSheetState.conversationSheetContent != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Surface(
                    shadowElevation = elevationState,
                    color = MaterialTheme.wireColorScheme.background
                ) {
                    WireTabRow(
                        tabs = GroupConversationDetailsTabItem.entries,
                        selectedTabIndex = currentTabState,
                        onTabChange = { scope.launch { pagerState.animateScrollToPage(it) } },
                        modifier = Modifier.padding(top = MaterialTheme.wireDimensions.spacing16x),
                        divider = {} // no divider
                    )
                }
            }
        },
        content = {
            var focusedTabIndex: Int by remember { mutableStateOf(initialPageIndex) }
            val keyboardController = LocalSoftwareKeyboardController.current
            val focusManager = LocalFocusManager.current

            CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                ) { pageIndex ->
                    when (GroupConversationDetailsTabItem.entries[pageIndex]) {
                        GroupConversationDetailsTabItem.OPTIONS -> GroupConversationOptions(
                            lazyListState = lazyListStates[pageIndex],
                            onEditGuestAccess = onEditGuestAccess,
                            onEditSelfDeletingMessages = onEditSelfDeletingMessages,
                            onEditGroupName = onEditGroupName
                        )

                        GroupConversationDetailsTabItem.PARTICIPANTS -> GroupConversationParticipants(
                            groupParticipantsState = groupParticipantsState,
                            openFullListPressed = openFullListPressed,
                            onAddParticipantsPressed = onAddParticipantsPressed,
                            onProfilePressed = onProfilePressed,
                            lazyListState = lazyListStates[pageIndex]
                        )
                    }
                }

                LaunchedEffect(pagerState.isScrollInProgress, focusedTabIndex, pagerState.currentPage) {
                    if (!pagerState.isScrollInProgress && focusedTabIndex != pagerState.currentPage) {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        focusedTabIndex = pagerState.currentPage
                    }
                }
            }
        }
    )

    WireModalSheetLayout(
        sheetState = sheetState,
        coroutineScope = rememberCoroutineScope(),
        sheetContent = {
            ConversationSheetContent(
                isBottomSheetVisible = getBottomSheetVisibility,
                conversationSheetState = conversationSheetState,
                onMutingConversationStatusChange = {
                    conversationSheetContent?.let {
                        bottomSheetEventsHandler.onMutingConversationStatusChange(
                            conversationSheetState.conversationId,
                            conversationSheetState.conversationSheetContent!!.mutingConversationState,
                            closeBottomSheetAndShowSnackbarMessage
                        )
                    }
                },
                addConversationToFavourites = bottomSheetEventsHandler::onAddConversationToFavourites,
                moveConversationToFolder = bottomSheetEventsHandler::onMoveConversationToFolder,
                updateConversationArchiveStatus = {
                    // Only show the confirmation dialog if the conversation is not archived
                    if (!it.isArchived) {
                        archiveConversationDialogState.show(it)
                    } else {
                        bottomSheetEventsHandler.updateConversationArchiveStatus(
                            dialogState = it,
                            onMessage = closeBottomSheetAndShowSnackbarMessage
                        )
                    }
                },
                clearConversationContent = clearConversationDialogState::show,
                blockUser = {},
                unblockUser = {},
                leaveGroup = leaveGroupDialogState::show,
                deleteGroup = deleteGroupDialogState::show
            )
        }
    )

    DeleteConversationGroupDialog(
        isLoading = isLoading,
        dialogState = deleteGroupDialogState,
        onDeleteGroup = onDeleteGroup
    )

    LeaveConversationGroupDialog(
        dialogState = leaveGroupDialogState,
        isLoading = isLoading,
        onLeaveGroup = onLeaveGroup
    )

    ClearConversationContentDialog(
        dialogState = clearConversationDialogState,
        isLoading = isLoading,
        onClearConversationContent = {
            bottomSheetEventsHandler.onClearConversationContent(dialogState = it, onMessage = closeBottomSheetAndShowSnackbarMessage)
        }
    )

    ArchiveConversationDialog(
        dialogState = archiveConversationDialogState,
        onArchiveButtonClicked = {
            bottomSheetEventsHandler.updateConversationArchiveStatus(dialogState = it, onMessage = closeBottomSheetAndShowSnackbarMessage)
        }
    )
}

enum class GroupConversationDetailsTabItem(@StringRes override val titleResId: Int) : TabItem {
    OPTIONS(R.string.conversation_details_options_tab),
    PARTICIPANTS(R.string.conversation_details_participants_tab);
}

@Preview
@Composable
fun PreviewGroupConversationDetails() {
    WireTheme(isPreview = true) {
        GroupConversationDetailsContent(
            conversationSheetContent = null,
            bottomSheetEventsHandler = GroupConversationDetailsBottomSheetEventsHandler.PREVIEW,
            onBackPressed = {},
            openFullListPressed = {},
            onProfilePressed = {},
            onAddParticipantsPressed = {},
            onLeaveGroup = {},
            onDeleteGroup = {},
            groupParticipantsState = GroupConversationParticipantsState.PREVIEW,
            isLoading = false,
            onEditGroupName = {},
            onEditSelfDeletingMessages = {},
            onEditGuestAccess = {},
            onSearchConversationMessagesClick = {}
        )
    }
}
