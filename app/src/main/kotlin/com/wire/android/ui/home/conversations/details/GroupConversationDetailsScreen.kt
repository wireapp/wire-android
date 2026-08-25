/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
 */

package com.wire.android.ui.home.conversations.details

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wire.android.R
import com.wire.android.ui.common.CollapsingTopBarScaffold
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.common.LoadingWireTabRow
import com.wire.android.ui.common.MLSVerifiedIcon
import com.wire.android.ui.common.MoreOptionIcon
import com.wire.android.ui.common.ProteusVerifiedIcon
import com.wire.android.ui.common.TabItem
import com.wire.android.ui.common.VisibilityState
import com.wire.android.ui.common.WireTabRow
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.bottomsheet.conversation.ConversationOptionsModalSheetLayout
import com.wire.android.ui.common.bottomsheet.conversation.ConversationSheetState
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.calculateCurrentTab
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.WireTopAppBarTitle
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptions
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsState
import com.wire.android.ui.home.conversations.details.options.LoadingGroupConversation
import com.wire.android.ui.home.conversations.details.participants.GroupConversationParticipants
import com.wire.android.ui.home.conversations.details.participants.GroupConversationParticipantsState
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.ui.home.conversations.folder.ConversationFoldersNavArgs
import com.wire.android.ui.home.conversations.info.ConversationAvatar
import com.wire.android.ui.home.conversationslist.showLegalHoldIndicator
import com.wire.android.ui.home.newconversation.channelaccess.ChannelAccessType
import com.wire.android.ui.home.newconversation.channelaccess.ChannelAddPermissionType
import com.wire.android.ui.legalhold.dialog.subject.LegalHoldSubjectConversationDialog
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Navigation-neutral renderer used by the typed Navigation 3 entry.
 *
 * Navigation callbacks and typed results are supplied by the Navigation 3 entry.
 */
@Suppress("ComposeViewModelForwarding")
@Composable
internal fun GroupConversationDetailsRouteScreen(
    viewModel: GroupConversationDetailsViewModel,
    actions: GroupConversationDetailsRouteScreenActions,
    renameResult: Boolean? = null,
) {
    val scope = rememberCoroutineScope()
    val resources = LocalContext.current.resources
    val snackbarHostState = LocalSnackbarHostState.current
    val sheetState = rememberWireModalSheetState<ConversationSheetState>()
    val groupOptions by viewModel.groupOptionsState.collectAsStateWithLifecycle()
    val renameFailureMessage = stringResource(id = R.string.error_unknown_message)
    val renameSuccessMessage = stringResource(id = R.string.conversation_options_renamed)

    LaunchedEffect(renameResult) {
        renameResult?.let {
            snackbarHostState.showSnackbar(if (it) renameSuccessMessage else renameFailureMessage)
        }
    }

    HandleActions(viewModel.actions) { action ->
        when (action) {
            is GroupConversationDetailsViewAction.Message -> sheetState.hide {
                snackbarHostState.showSnackbar(action.text.asString(resources))
            }
        }
    }

    GroupConversationDetailsContent(
        sheetState = sheetState,
        groupConversationOptionsState = groupOptions,
        viewModel = viewModel,
        onBackPressed = actions.onBackPressed,
        onProfilePressed = actions.onProfilePressed,
        onAddParticipantsPressed = {
            actions.onAddParticipants(
                groupOptions.isAppsAllowed,
                groupOptions.isSelfPartOfATeam,
                groupOptions.protocolInfo,
                groupOptions.shouldUseNewAppsUi,
            )
        },
        groupParticipantsState = viewModel.groupParticipantsState,
        onEditGuestAccess = {
            actions.onEditGuestAccess(
                groupOptions.isGuestAllowed,
                groupOptions.isAppsAllowed,
                groupOptions.isUpdatingGuestAllowed,
            )
        },
        onAppsAccessItemClicked = {
            actions.onAppsAccess(
                groupOptions.isGuestAllowed,
                groupOptions.isAppsAllowed,
                groupOptions.shouldUseNewAppsUi,
            )
        },
        onChannelAccessItemClicked = {
            actions.onChannelAccess(
                checkNotNull(groupOptions.channelAccessType),
                checkNotNull(groupOptions.channelAddPermissionType),
            )
        },
        onEditSelfDeletingMessages = actions.onEditSelfDeletingMessages,
        onEditGroupName = actions.onEditGroupName,
        isWireCellEnabled = groupOptions.isWireCellEnabled,
        onSearchConversationMessagesClick = {
            actions.onSearchConversationMessages(
                groupOptions.isWireCellEnabled && groupOptions.isWireCellFeatureEnabled,
                groupOptions.groupName,
            )
        },
        onConversationMediaClick = {
            actions.onConversationMedia(groupOptions.isWireCellEnabled, groupOptions.groupName)
        },
        isAbandonedOneOnOneConversation =
        groupOptions.isAbandonedOneOnOneConversation(viewModel.groupParticipantsState.data.allCount),
        onMoveToFolder = { args ->
            actions.onMoveToFolder(args) { message ->
                scope.launch { snackbarHostState.showSnackbar(message) }
            }
        },
        onLeftConversation = {
            actions.onConversationCompleted(GroupConversationActionType.LEAVE_GROUP, groupOptions.groupName)
        },
        onDeletedConversation = {
            actions.onConversationCompleted(GroupConversationActionType.DELETE_GROUP, groupOptions.groupName)
        },
        onPromoteAdmin = actions.onPromoteAdmin,
        openConversationDebugMenu = actions.onOpenConversationDebugMenu,
        isScreenLoading = viewModel.isFetchingInitialData,
    )
}

internal data class GroupConversationDetailsRouteScreenActions(
    val onBackPressed: () -> Unit,
    val onProfilePressed: (UIParticipant) -> Unit,
    val onAddParticipants: (Boolean, Boolean, Conversation.ProtocolInfo, Boolean) -> Unit,
    val onEditGuestAccess: (Boolean, Boolean, Boolean) -> Unit,
    val onAppsAccess: (Boolean, Boolean, Boolean) -> Unit,
    val onChannelAccess: (ChannelAccessType, ChannelAddPermissionType) -> Unit,
    val onEditSelfDeletingMessages: () -> Unit,
    val onEditGroupName: () -> Unit,
    val onSearchConversationMessages: (Boolean, String) -> Unit,
    val onConversationMedia: (Boolean, String) -> Unit,
    val onMoveToFolder: (ConversationFoldersNavArgs, onMessage: (String) -> Unit) -> Unit,
    val onConversationCompleted: (GroupConversationActionType, String) -> Unit,
    val onPromoteAdmin: (ConversationId, List<UserId>) -> Unit,
    val onOpenConversationDebugMenu: (ConversationId) -> Unit,
)

@Suppress("CyclomaticComplexMethod")
@SuppressLint("ComposeViewModelForwarding")
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupConversationDetailsContent(
    groupConversationOptionsState: GroupConversationOptionsState,
    sheetState: WireModalSheetState<ConversationSheetState>,
    onBackPressed: () -> Unit,
    onProfilePressed: (UIParticipant) -> Unit,
    onAddParticipantsPressed: () -> Unit,
    onEditGuestAccess: () -> Unit,
    onAppsAccessItemClicked: () -> Unit,
    onChannelAccessItemClicked: () -> Unit,
    onEditSelfDeletingMessages: () -> Unit,
    onEditGroupName: () -> Unit,
    groupParticipantsState: GroupConversationParticipantsState,
    isAbandonedOneOnOneConversation: Boolean,
    isWireCellEnabled: Boolean,
    onSearchConversationMessagesClick: () -> Unit,
    onConversationMediaClick: () -> Unit,
    viewModel: GroupConversationDetailsViewModel? = null,
    onMoveToFolder: (ConversationFoldersNavArgs) -> Unit = {},
    onLeftConversation: () -> Unit = {},
    onDeletedConversation: () -> Unit = {},
    onPromoteAdmin: (ConversationId, List<UserId>) -> Unit = { _, _ -> },
    openConversationDebugMenu: (ConversationId) -> Unit = {},
    initialPageIndex: GroupConversationDetailsTabItem = GroupConversationDetailsTabItem.OPTIONS,
    isScreenLoading: StateFlow<Boolean> = MutableStateFlow(false),
) {
    val scope = rememberCoroutineScope()
    val lazyListStates: List<LazyListState> = GroupConversationDetailsTabItem.entries.map { rememberLazyListState() }
    val pagerState = rememberPagerState(
        initialPage = initialPageIndex.ordinal,
        pageCount = { GroupConversationDetailsTabItem.entries.size }
    )
    val currentTabState by remember { derivedStateOf { pagerState.calculateCurrentTab() } }
    val legalHoldSubjectDialogState = rememberVisibilityState<Unit>()
    val isLoading by isScreenLoading.collectAsStateWithLifecycle()

    CollapsingTopBarScaffold(
        topBarHeader = {
            WireCenterAlignedTopAppBar(
                elevation = dimensions().spacing0x, // CollapsingTopBarScaffold already manages elevation
                titleContent = {
                    WireTopAppBarTitle(
                        title = stringResource(R.string.conversation_details_title),
                        style = MaterialTheme.wireTypography.title01,
                        maxLines = 2
                    )
                    VerificationInfo(groupConversationOptionsState)
                },
                navigationIconType = NavigationIconType.Close(R.string.content_description_conversation_details_close_btn),
                onNavigationPressed = onBackPressed,
                actions = {
                    MoreOptionIcon(
                        contentDescription = R.string.content_description_conversation_details_more_btn,
                        onButtonClicked = {
                            sheetState.show(ConversationSheetState(groupConversationOptionsState.conversationId))
                        }
                    )
                }
            )
        },
        topBarCollapsing = {
            val avatarData = if (groupConversationOptionsState.isChannel) {
                ConversationAvatar.Group.Channel(
                    conversationId = groupConversationOptionsState.conversationId,
                    isPrivate = groupConversationOptionsState.channelAccessType == ChannelAccessType.PRIVATE,
                )
            } else {
                ConversationAvatar.Group.Regular(conversationId = groupConversationOptionsState.conversationId)
            }

            AnimatedContent(
                targetState = isLoading,
                transitionSpec = {
                    val enter = fadeIn(tween(durationMillis = 500, delayMillis = 100))
                    val exit = fadeOut()
                    enter.togetherWith(exit)
                },
                label = "TopBarContent"
            ) { loading ->
                if (loading) {
                    LoadingGroupConversationDetailsTopBarCollapsing(
                        modifier = Modifier.padding(bottom = MaterialTheme.wireDimensions.spacing16x)
                    )
                } else {
                    GroupConversationDetailsTopBarCollapsing(
                        title = groupConversationOptionsState.groupName,
                        totalParticipants = groupParticipantsState.data.allCount,
                        conversationAvatar = avatarData,
                        onSearchConversationMessagesClick = onSearchConversationMessagesClick,
                        onConversationMediaClick = onConversationMediaClick,
                        isUnderLegalHold = groupConversationOptionsState.legalHoldStatus.showLegalHoldIndicator(),
                        isWireCellEnabled = isWireCellEnabled,
                        onLegalHoldLearnMoreClick = remember { { legalHoldSubjectDialogState.show(Unit) } },
                        modifier = Modifier.padding(bottom = MaterialTheme.wireDimensions.spacing16x)
                    )
                }
            }
        },
        topBarFooter = {
            Crossfade(isLoading) { loading ->
                if (loading) {
                    LoadingWireTabRow()
                } else {
                    WireTabRow(
                        tabs = GroupConversationDetailsTabItem.entries,
                        selectedTabIndex = currentTabState,
                        onTabChange = { scope.launch { pagerState.animateScrollToPage(it) } },
                    )
                }
            }
        },
        bottomBar = {
            AnimatedContent(
                targetState = currentTabState,
                label = "Conversation details bottom bar crossfade",
                transitionSpec = {
                    val enter = fadeIn(tween(durationMillis = 500, delayMillis = 100))
                    val exit = fadeOut()
                    enter.togetherWith(exit)
                },
                modifier = Modifier.fillMaxWidth()
            ) { currentTabState ->
                Surface(
                    shadowElevation = MaterialTheme.wireDimensions.bottomNavigationShadowElevation,
                    color = MaterialTheme.wireColorScheme.background,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    when (GroupConversationDetailsTabItem.entries[currentTabState]) {
                        GroupConversationDetailsTabItem.OPTIONS -> {
                            // no bottom bar for options tab
                        }

                        GroupConversationDetailsTabItem.PARTICIPANTS -> {
                            val shouldShowAddParticipantsButton = groupConversationOptionsState.canSelfAddParticipants
                                    && !isAbandonedOneOnOneConversation
                            if (shouldShowAddParticipantsButton) {
                                Box(modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x)) {
                                    WirePrimaryButton(
                                        text = stringResource(R.string.conversation_details_conversation_participants_add),
                                        onClick = onAddParticipantsPressed,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        contentLazyListState = lazyListStates[currentTabState],
    ) {
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LoadingGroupConversation()
        }

        var focusedTabIndex: Int by remember { mutableStateOf(initialPageIndex.ordinal) }
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current

        AnimatedVisibility(
            visible = !isLoading,
            enter = fadeIn(
                animationSpec = tween(durationMillis = 500, delayMillis = 100)
            ),
            exit = fadeOut()
        ) {
            CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    when (GroupConversationDetailsTabItem.entries[pageIndex]) {
                        GroupConversationDetailsTabItem.OPTIONS -> GroupConversationOptions(
                            lazyListState = lazyListStates[pageIndex],
                            onEditGuestAccess = onEditGuestAccess,
                            onAppsAccessItemClicked = onAppsAccessItemClicked,
                            onChannelAccessItemClicked = onChannelAccessItemClicked,
                            onEditSelfDeletingMessages = onEditSelfDeletingMessages,
                            viewModel = requireNotNull(viewModel) {
                                "GroupConversationDetailsContent requires GroupConversationDetailsViewModel outside preview"
                            },
                            onEditGroupName = onEditGroupName
                        )

                        GroupConversationDetailsTabItem.PARTICIPANTS -> GroupConversationParticipants(
                            groupParticipantsState = groupParticipantsState,
                            onProfilePressed = onProfilePressed,
                            lazyListState = lazyListStates[pageIndex],
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
    }

    ConversationOptionsModalSheetLayout(
        sheetState = sheetState,
        openConversationFolders = onMoveToFolder,
        onLeftConversation = onLeftConversation,
        onDeletedConversation = onDeletedConversation,
        onPromoteAdmin = onPromoteAdmin,
        openConversationDebugMenu = openConversationDebugMenu,
    )

    VisibilityState(legalHoldSubjectDialogState) {
        LegalHoldSubjectConversationDialog(legalHoldSubjectDialogState::dismiss)
    }
}

@Composable
private fun VerificationInfo(
    groupConversationOptionsState: GroupConversationOptionsState
) {
    val isProteusVerified = groupConversationOptionsState.proteusVerificationStatus == Conversation.VerificationStatus.VERIFIED
    val isMlsVerified = groupConversationOptionsState.mlsVerificationStatus == Conversation.VerificationStatus.VERIFIED
    val isProteusProtocol = groupConversationOptionsState.protocolInfo == Conversation.ProtocolInfo.Proteus

    if (isProteusVerified && (isProteusProtocol || !isMlsVerified)) {
        ProteusVerifiedLabel()
    } else if (isMlsVerified) {
        MLSVerifiedLabel()
    }
}

@Composable
private fun MLSVerifiedLabel() {
    VerifiedLabel(
        stringResource(id = R.string.label_conversations_details_verified_mls).uppercase(),
        MaterialTheme.wireColorScheme.positive
    ) { MLSVerifiedIcon() }
}

@Composable
private fun ProteusVerifiedLabel() {
    VerifiedLabel(
        stringResource(id = R.string.label_conversations_details_verified_proteus).uppercase(),
        MaterialTheme.wireColorScheme.primary
    ) { ProteusVerifiedIcon() }
}

@Composable
private fun VerifiedLabel(text: String, color: Color, icon: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = Modifier
            .padding(top = dimensions().spacing4x)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            modifier = Modifier.padding(
                start = dimensions().spacing6x,
                end = dimensions().spacing6x
            ),
            text = text,
            style = MaterialTheme.wireTypography.label01,
            color = color,
            overflow = TextOverflow.Ellipsis
        )
        icon()
    }
}

enum class GroupConversationDetailsTabItem(@StringRes val titleResId: Int) : TabItem {
    OPTIONS(R.string.conversation_details_options_tab),
    PARTICIPANTS(R.string.conversation_details_participants_tab);

    override val title: UIText = UIText.StringResource(titleResId)
}

@PreviewMultipleThemes
@Composable
fun PreviewGroupConversationDetails() {
    WireTheme {
        GroupConversationDetailsContent(
            groupConversationOptionsState = GroupConversationOptionsState(ConversationId("v", "d")),
            sheetState = rememberWireModalSheetState(),
            onBackPressed = {},
            onProfilePressed = {},
            onAddParticipantsPressed = {},
            onEditGuestAccess = {},
            onAppsAccessItemClicked = {},
            onChannelAccessItemClicked = {},
            onEditSelfDeletingMessages = {},
            onEditGroupName = {},
            groupParticipantsState = GroupConversationParticipantsState.PREVIEW,
            isAbandonedOneOnOneConversation = false,
            isWireCellEnabled = false,
            onSearchConversationMessagesClick = {},
            onConversationMediaClick = {},
            onMoveToFolder = {},
            onLeftConversation = {},
            onDeletedConversation = {},
            initialPageIndex = GroupConversationDetailsTabItem.PARTICIPANTS
        )
    }
}
