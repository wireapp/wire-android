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

package com.wire.android.ui.userprofile.other

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.ramcosta.composedestinations.result.ResultRecipient
import com.ramcosta.composedestinations.spec.DestinationStyle
import com.wire.android.R
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireDestination
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.common.CollapsingTopBarScaffold
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.common.MoreOptionIcon
import com.wire.android.ui.common.TabItem
import com.wire.android.ui.common.VisibilityState
import com.wire.android.ui.common.WireTabRow
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.bottomsheet.conversation.ConversationOptionsModalSheetLayout
import com.wire.android.ui.common.bottomsheet.conversation.ConversationSheetState
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.calculateCurrentTab
import com.wire.android.ui.common.dialogs.UserNotFoundDialog
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.WireTopAppBarTitle
import com.wire.android.ui.common.visbility.VisibilityState
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.connection.ConnectionActionButton
import com.wire.android.ui.debug.conversation.DebugConversationScreenNavArgs
import com.wire.android.ui.destinations.ConversationFoldersScreenDestination
import com.wire.android.ui.destinations.ConversationMediaScreenDestination
import com.wire.android.ui.destinations.ConversationScreenDestination
import com.wire.android.ui.destinations.DebugConversationScreenDestination
import com.wire.android.ui.destinations.DeviceDetailsScreenDestination
import com.wire.android.ui.destinations.SearchConversationMessagesScreenDestination
import com.wire.android.ui.home.conversations.details.SearchAndMediaRow
import com.wire.android.ui.home.conversations.folder.ConversationFoldersNavArgs
import com.wire.android.ui.home.conversations.folder.ConversationFoldersNavBackArgs
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.legalhold.banner.LegalHoldSubjectBanner
import com.wire.android.ui.legalhold.dialog.subject.LegalHoldSubjectProfileDialog
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.ui.userprofile.common.EditableState
import com.wire.android.ui.userprofile.common.UserProfileInfo
import com.wire.android.ui.userprofile.group.RemoveConversationMemberState
import com.wire.android.ui.userprofile.other.bottomsheet.EditGroupRoleBottomSheet
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.ConnectionState
import io.github.esentsov.PackagePrivate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

@WireDestination(
    navArgsDelegate = OtherUserProfileNavArgs::class,
    style = DestinationStyle.Runtime::class, // default should be PopUpNavigationAnimation
)
@Composable
fun OtherUserProfileScreen(
    navigator: Navigator,
    navArgs: OtherUserProfileNavArgs,
    resultNavigator: ResultBackNavigator<String>,
    conversationFoldersScreenResultRecipient:
    ResultRecipient<ConversationFoldersScreenDestination, ConversationFoldersNavBackArgs>,
    viewModel: OtherUserProfileScreenViewModel = hiltViewModel()
) {
    val snackbarHostState = LocalSnackbarHostState.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val conversationId = viewModel.state.conversationId
    val onSearchConversationMessagesClick: () -> Unit = {
        conversationId?.let {
            navigator.navigate(
                NavigationCommand(
                    SearchConversationMessagesScreenDestination(
                        conversationId = it
                    )
                )
            )
        }
    }

    val onConversationMediaClick: () -> Unit = {
        conversationId?.let {
            navigator.navigate(
                NavigationCommand(
                    ConversationMediaScreenDestination(
                        conversationId = it
                    )
                )
            )
        }
    }

    val legalHoldSubjectDialogState = rememberVisibilityState<Unit>()
    val conversationOptionsSheetState = rememberWireModalSheetState<ConversationSheetState>()
    val changeRoleSheetState = rememberWireModalSheetState<OtherUserProfileGroupState>()

    OtherProfileScreenContent(
        scope = scope,
        state = viewModel.state,
        conversationOptionsSheetState = conversationOptionsSheetState,
        changeRoleSheetState = changeRoleSheetState,
        removeMemberDialogState = viewModel.removeConversationMemberDialogState,
        eventsHandler = viewModel as OtherUserProfileEventsHandler,
        onChangeMemberRole = viewModel::onChangeMemberRole,
        onIgnoreConnectionRequest = {
            resultNavigator.setResult(it)
            resultNavigator.navigateBack()
        },
        onOpenConversation = {
            navigator.navigate(
                NavigationCommand(
                    ConversationScreenDestination(it),
                    BackStackMode.UPDATE_EXISTED
                )
            )
        },
        onOpenDeviceDetails = {
            navigator.navigate(
                NavigationCommand(
                    DeviceDetailsScreenDestination(
                        navArgs.userId,
                        it.clientId
                    )
                )
            )
        },
        onSearchConversationMessagesClick = onSearchConversationMessagesClick,
        navigateBack = navigator::navigateBack,
        onConversationMediaClick = onConversationMediaClick,
        onLegalHoldLearnMoreClick = remember { { legalHoldSubjectDialogState.show(Unit) } },
        onMoveToFolder = {
            navigator.navigate(NavigationCommand(ConversationFoldersScreenDestination(it)))
        },
        openConversationDebugMenu = { conversationId ->
            navigator.navigate(
                NavigationCommand(
                    DebugConversationScreenDestination(
                        navArgs = DebugConversationScreenNavArgs(conversationId)
                    )
                )
            )
        },
    )

    HandleActions(viewModel.actions) { action ->
        fun hideSheets(onSuccess: suspend () -> Unit) {
            conversationOptionsSheetState.hide { changeRoleSheetState.hide { onSuccess() } }
        }
        when (action) {
            is OtherUserProfileViewAction.Message -> hideSheets {
                snackbarHostState.showSnackbar(action.message.uiText.asString(context.resources))
            }
        }
    }

    VisibilityState(legalHoldSubjectDialogState) {
        LegalHoldSubjectProfileDialog(
            viewModel.state.userName,
            legalHoldSubjectDialogState::dismiss
        )
    }

    if (viewModel.state.errorLoadingUser != null) {
        UserNotFoundDialog(onActionButtonClicked = navigator::navigateBack)
    }

    conversationFoldersScreenResultRecipient.onNavResult { result ->
        when (result) {
            NavResult.Canceled -> {}
            is NavResult.Value -> {
                scope.launch {
                    snackbarHostState.showSnackbar(result.value.message)
                }
            }
        }
    }
}

@SuppressLint("UnusedCrossfadeTargetStateParameter", "LongParameterList")
@Composable
fun OtherProfileScreenContent(
    scope: CoroutineScope,
    state: OtherUserProfileState,
    eventsHandler: OtherUserProfileEventsHandler,
    conversationOptionsSheetState: WireModalSheetState<ConversationSheetState> = rememberWireModalSheetState(),
    changeRoleSheetState: WireModalSheetState<OtherUserProfileGroupState> = rememberWireModalSheetState(),
    removeMemberDialogState: VisibilityState<RemoveConversationMemberState> = rememberVisibilityState(),
    onChangeMemberRole: (role: Conversation.Member.Role) -> Unit = {},
    onSearchConversationMessagesClick: () -> Unit = {},
    onIgnoreConnectionRequest: (String) -> Unit = {},
    onOpenConversation: (ConversationId) -> Unit = {},
    onOpenDeviceDetails: (Device) -> Unit = {},
    onConversationMediaClick: () -> Unit = {},
    navigateBack: () -> Unit = {},
    onLegalHoldLearnMoreClick: () -> Unit = {},
    onMoveToFolder: (ConversationFoldersNavArgs) -> Unit = {},
    openConversationDebugMenu: (ConversationId) -> Unit = {},
) {
    val otherUserProfileScreenState = rememberOtherUserProfileScreenState()
    val tabItems by remember(state) {
        derivedStateOf {
            listOfNotNull(
                state.groupState?.let { OtherUserProfileTabItem.GROUP },
                OtherUserProfileTabItem.DETAILS,
                OtherUserProfileTabItem.DEVICES
            )
        }
    }
    val initialPage = 0
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { tabItems.size })
    val lazyListStates = OtherUserProfileTabItem.entries.associateWith { rememberLazyListState() }
    val currentTabState by remember(state, pagerState) {
        derivedStateOf { if (state.isDataLoading) 0 else pagerState.calculateCurrentTab() }
    }

    CollapsingTopBarScaffold(
        topBarHeader = {
            TopBarHeader(
                state = state,
                elevation = dimensions().spacing0x, // CollapsingTopBarScaffold already manages elevation
                onNavigateBack = navigateBack,
                openConversationBottomSheet = { conversationOptionsSheetState.show(ConversationSheetState(it)) }
            )
        },
        topBarCollapsing = {
            TopBarCollapsing(
                state = state,
                onSearchConversationMessagesClick = onSearchConversationMessagesClick,
                onConversationMediaClick = onConversationMediaClick,
                onLegalHoldLearnMoreClick = onLegalHoldLearnMoreClick,
            )
        },
        topBarFooter = {
            TopBarFooter(
                state = state,
                pagerState = pagerState,
                tabItems = tabItems,
                currentTab = currentTabState,
                scope = scope
            )
        },
        contentLazyListState = lazyListStates[tabItems[currentTabState]],
        content = {
            Content(
                state = state,
                pagerState = pagerState,
                tabItems = tabItems,
                otherUserProfileScreenState = otherUserProfileScreenState,
                lazyListStates = lazyListStates,
                openChangeRoleBottomSheet = { changeRoleSheetState.show(it) },
                openRemoveConversationMemberDialog = removeMemberDialogState::show,
                getOtherUserClients = eventsHandler::observeClientList,
                onDeviceClick = onOpenDeviceDetails
            )
        },
        bottomBar = {
            ContentFooter(
                state,
                onIgnoreConnectionRequest,
                onOpenConversation
            )
        },
        collapsingEnabled = state.connectionState != ConnectionState.BLOCKED
    )

    ConversationOptionsModalSheetLayout(
        sheetState = conversationOptionsSheetState,
        openConversationFolders = onMoveToFolder,
        openConversationDebugMenu = openConversationDebugMenu
    )
    EditGroupRoleBottomSheet(
        sheetState = changeRoleSheetState,
        changeMemberRole = onChangeMemberRole,
    )
    RemoveConversationMemberDialog(
        dialogState = removeMemberDialogState,
        onRemoveConversationMember = eventsHandler::onRemoveConversationMember,
    )
}

@Composable
private fun TopBarHeader(
    state: OtherUserProfileState,
    elevation: Dp,
    onNavigateBack: () -> Unit,
    openConversationBottomSheet: (ConversationId) -> Unit
) {
    val navigationIconType = if (state.groupState != null) {
        NavigationIconType.Close(R.string.content_description_user_profile_close_btn)
    } else if (state.connectionState == ConnectionState.PENDING || state.connectionState == ConnectionState.IGNORED) {
        NavigationIconType.Close(R.string.content_description_connection_request_close_btn)
    } else {
        NavigationIconType.Close()
    }

    WireCenterAlignedTopAppBar(
        onNavigationPressed = onNavigateBack,
        navigationIconType = navigationIconType,
        titleContent = {
            WireTopAppBarTitle(
                title = stringResource(id = R.string.user_profile_title),
                style = MaterialTheme.wireTypography.title01,
                maxLines = 2
            )
        },
        elevation = elevation,
        actions = {
            if (state.activeOneOnOneConversationId != null) {
                MoreOptionIcon(
                    contentDescription = R.string.content_description_user_profile_more_btn,
                    onButtonClicked = { openConversationBottomSheet(state.activeOneOnOneConversationId) },
                    state = if (state.isMetadataEmpty()) WireButtonState.Disabled else WireButtonState.Default
                )
            }
        }
    )
}

@Composable
private fun TopBarCollapsing(
    state: OtherUserProfileState,
    onSearchConversationMessagesClick: () -> Unit,
    onConversationMediaClick: () -> Unit = {},
    onLegalHoldLearnMoreClick: () -> Unit = {},
) {
    Crossfade(
        targetState = state,
        label = "OtherUserProfileScreenTopBarCollapsing"
    ) { targetState ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = dimensions().spacing16x),
        ) {
            UserProfileInfo(
                userId = targetState.userId,
                isLoading = targetState.isAvatarLoading,
                avatarAsset = targetState.userAvatarAsset,
                fullName = targetState.fullName,
                userName = targetState.userName,
                teamName = targetState.teamName,
                membership = targetState.membership,
                editableState = EditableState.NotEditable,
                connection = targetState.connectionState,
                isProteusVerified = targetState.isProteusVerified,
                isMLSVerified = targetState.isMLSVerified,
                expiresAt = targetState.expiresAt,
                accentId = targetState.accentId
            )
            if (state.isUnderLegalHold) {
                LegalHoldSubjectBanner(
                    onClick = onLegalHoldLearnMoreClick,
                    modifier = Modifier.padding(top = dimensions().spacing8x)
                )
            }
            if (state.shouldShowSearchButton()) {
                VerticalSpace.x24()
                SearchAndMediaRow(
                    isWireCellEnabled = false,
                    onSearchConversationMessagesClick = onSearchConversationMessagesClick,
                    onConversationMediaClick = onConversationMediaClick
                )
            }
        }
    }
}

@Composable
private fun TopBarFooter(
    state: OtherUserProfileState,
    pagerState: PagerState,
    tabItems: List<OtherUserProfileTabItem>,
    currentTab: Int,
    scope: CoroutineScope
) {
    if (state.connectionState == ConnectionState.ACCEPTED) {
        AnimatedVisibility(
            visible = !state.isDataLoading,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            WireTabRow(
                tabs = tabItems,
                selectedTabIndex = currentTab,
                onTabChange = { scope.launch { pagerState.animateScrollToPage(it) } },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Content(
    state: OtherUserProfileState,
    pagerState: PagerState,
    tabItems: List<OtherUserProfileTabItem>,
    otherUserProfileScreenState: OtherUserProfileScreenState,
    lazyListStates: Map<OtherUserProfileTabItem, LazyListState>,
    openChangeRoleBottomSheet: (OtherUserProfileGroupState) -> Unit,
    openRemoveConversationMemberDialog: (RemoveConversationMemberState) -> Unit,
    getOtherUserClients: () -> Unit,
    onDeviceClick: (Device) -> Unit
) {

    Crossfade(targetState = tabItems to state, label = "OtherUserProfile") { (tabItems, state) ->
        Column {
            if (!state.isDataLoading && !state.isTemporaryUser()) {
                OtherUserConnectionStatusInfo(state.connectionState, state.membership)
                OtherUserConnectionUnverifiedWarning(state.fullName, state.connectionState)
            }
            when {
                state.isDataLoading || state.botService != null -> Box {} // no content visible while loading
                state.connectionState == ConnectionState.ACCEPTED -> {
                    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                        HorizontalPager(
                            modifier = Modifier.fillMaxSize(),
                            state = pagerState,
                        ) { pageIndex ->
                            when (val tabItem = tabItems[pageIndex]) {
                                OtherUserProfileTabItem.DETAILS ->
                                    OtherUserProfileDetails(
                                        state = state,
                                        otherUserProfileScreenState = otherUserProfileScreenState,
                                        lazyListState = lazyListStates[tabItem]!!
                                    )

                                OtherUserProfileTabItem.GROUP ->
                                    OtherUserProfileGroup(
                                        state = state.groupState!!, // groupState is guaranteed to be non-null here
                                        isRoleEditable = state.isRoleEditable(),
                                        onRemoveFromConversation = {
                                            openRemoveConversationMemberDialog(
                                                RemoveConversationMemberState(it, state.fullName, state.userName, state.userId)
                                            )
                                        },
                                        openChangeRoleBottomSheet = openChangeRoleBottomSheet,
                                        lazyListState = lazyListStates[tabItem]!!,
                                    )

                                OtherUserProfileTabItem.DEVICES -> {
                                    getOtherUserClients()
                                    OtherUserDevicesScreen(
                                        lazyListState = lazyListStates[tabItem]!!,
                                        state = state,
                                        onDeviceClick = onDeviceClick
                                    )
                                }
                            }
                        }
                    }
                }

                state.groupState != null -> {
                    OtherUserProfileGroup(
                        state = state.groupState,
                        isRoleEditable = state.isRoleEditable(),
                        onRemoveFromConversation = {
                            openRemoveConversationMemberDialog(
                                RemoveConversationMemberState(it, state.fullName, state.userName, state.userId)
                            )
                        },
                        openChangeRoleBottomSheet = openChangeRoleBottomSheet,
                        lazyListState = lazyListStates[OtherUserProfileTabItem.DETAILS]!!,
                    )
                }

                else -> Box {}
            }
        }
    }
}

@SuppressLint("ComposeModifierMissing")
@PackagePrivate
@Composable
fun ContentFooter(
    state: OtherUserProfileState,
    onIgnoreConnectionRequest: (String) -> Unit = {},
    onOpenConversation: (ConversationId) -> Unit = {}
) {
    AnimatedVisibility(
        visible = !state.isDataLoading,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        // TODO show open conversation button for service bots after AR-2135
        val isNotTemporaryAndNotDeleted = !state.isTemporaryUser() && !state.isDeletedUser
        if (!state.isMetadataEmpty() && state.membership != Membership.Service && isNotTemporaryAndNotDeleted) {
            Surface(
                shadowElevation = dimensions().bottomNavigationShadowElevation,
                color = MaterialTheme.wireColorScheme.background
            ) {
                Box(modifier = Modifier.padding(all = dimensions().spacing16x)) {
                    ConnectionActionButton(
                        state.userId,
                        state.userName,
                        state.fullName,
                        state.connectionState,
                        state.isConversationStarted,
                        onConnectionRequestIgnored = onIgnoreConnectionRequest,
                        onOpenConversation = onOpenConversation
                    )
                }
            }
        }
    }
}

enum class OtherUserProfileTabItem(@StringRes val titleResId: Int) : TabItem {
    GROUP(R.string.user_profile_conversation_tab),
    DETAILS(R.string.user_profile_details_tab),
    DEVICES(R.string.user_profile_devices_tab);

    override val title: UIText = UIText.StringResource(titleResId)
}

@Composable
@PreviewMultipleThemes
fun PreviewOtherProfileScreenGroupMemberContent() {
    WireTheme {
        OtherProfileScreenContent(
            scope = rememberCoroutineScope(),
            state = OtherUserProfileState.PREVIEW.copy(
                connectionState = ConnectionState.ACCEPTED,
                isUnderLegalHold = true,
            ),
            eventsHandler = OtherUserProfileEventsHandler.PREVIEW,
        )
    }
}

@Composable
@PreviewMultipleThemes
fun PreviewOtherProfileScreenContent() {
    WireTheme {
        OtherProfileScreenContent(
            scope = rememberCoroutineScope(),
            state = OtherUserProfileState.PREVIEW.copy(
                connectionState = ConnectionState.ACCEPTED,
                isUnderLegalHold = true,
                groupState = null
            ),
            eventsHandler = OtherUserProfileEventsHandler.PREVIEW,
        )
    }
}

@Composable
@PreviewMultipleThemes
fun PreviewOtherProfileScreenContentNotConnected() {
    WireTheme {
        OtherProfileScreenContent(
            scope = rememberCoroutineScope(),
            state = OtherUserProfileState.PREVIEW.copy(
                connectionState = ConnectionState.CANCELLED,
                isUnderLegalHold = true,
            ),
            eventsHandler = OtherUserProfileEventsHandler.PREVIEW,
        )
    }
}

@Composable
@PreviewMultipleThemes
fun PreviewOtherProfileScreenTempUser() {
    WireTheme {
        OtherProfileScreenContent(
            scope = rememberCoroutineScope(),
            state = OtherUserProfileState.PREVIEW.copy(
                userName = "",
                connectionState = ConnectionState.CANCELLED,
                isUnderLegalHold = true,
                expiresAt = Instant.DISTANT_FUTURE
            ),
            eventsHandler = OtherUserProfileEventsHandler.PREVIEW,
        )
    }
}
