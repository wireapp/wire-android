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
package com.wire.android.ui.sharing

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.model.SnackBarMessage
import com.wire.android.model.UserAvatarData
import com.wire.android.navigation.Navigator
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.bottomsheet.MenuModalSheetLayout
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.search.SearchBarState
import com.wire.android.ui.common.topappbar.search.SearchTopBar
import com.wire.android.ui.home.FeatureFlagState
import com.wire.android.ui.home.conversations.media.preview.AssetPreview
import com.wire.android.ui.home.conversations.selfdeletion.SelfDeletionMapper.toSelfDeletionDuration
import com.wire.android.ui.home.conversations.selfdeletion.SelfDeletionMenuItems
import com.wire.android.ui.home.conversations.sendmessage.SendMessageViewModel
import com.wire.android.ui.home.conversationslist.common.ConversationList
import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.messagecomposer.SelfDeletionDuration
import com.wire.android.ui.home.newconversation.common.SendContentButton
import com.wire.android.ui.home.sync.FeatureFlagNotificationViewModel
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.extension.getActivity
import com.wire.android.util.ui.LinkText
import com.wire.android.util.ui.LinkTextData
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.util.isPositiveNotNull
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

@RootNavGraph
@Destination
@Composable
fun ImportMediaScreen(
    navigator: Navigator,
    featureFlagNotificationViewModel: FeatureFlagNotificationViewModel = hiltViewModel()
) {
    when (val fileSharingRestrictedState =
        featureFlagNotificationViewModel.featureFlagState.fileSharingRestrictedState) {
        FeatureFlagState.SharingRestrictedState.NO_USER -> {
            ImportMediaLoggedOutContent(
                fileSharingRestrictedState = fileSharingRestrictedState,
                navigateBack = navigator.finish
            )
        }

        FeatureFlagState.SharingRestrictedState.RESTRICTED_IN_TEAM -> {
            val importMediaViewModel: ImportMediaAuthenticatedViewModel = hiltViewModel()
            ImportMediaRestrictedContent(
                fileSharingRestrictedState = fileSharingRestrictedState,
                importMediaAuthenticatedState = importMediaViewModel.importMediaState,
                navigateBack = navigator.finish
            )
        }

        FeatureFlagState.SharingRestrictedState.NONE -> {
            val importMediaViewModel: ImportMediaAuthenticatedViewModel = hiltViewModel()
            val sendMessageViewModel: SendMessageViewModel = hiltViewModel()

            ImportMediaRegularContent(
                importMediaAuthenticatedState = importMediaViewModel.importMediaState,
                onSearchQueryChanged = importMediaViewModel::onSearchQueryChanged,
                onConversationClicked = importMediaViewModel::onConversationClicked,
                checkRestrictionsAndSendImportedMedia = {
                                                        // TODO KBX
//                    importMediaViewModel.checkRestrictionsAndSendImportedMedia {
//                        navigator.navigate(
//                            NavigationCommand(
//                                ConversationScreenDestination(it),
//                                BackStackMode.REMOVE_CURRENT
//                            )
//                        )
//                    }
                },
                onNewSelfDeletionTimerPicked = importMediaViewModel::onNewSelfDeletionTimerPicked,
                infoMessage = importMediaViewModel.infoMessage,
                navigateBack = navigator.finish,
            )
            val context = LocalContext.current
            LaunchedEffect(importMediaViewModel.importMediaState.importedAssets) {
                if (importMediaViewModel.importMediaState.importedAssets.isEmpty()) {
                    context.getActivity()
                        ?.let { importMediaViewModel.handleReceivedDataFromSharingIntent(it) }
                }
            }
        }

        null -> {
            // state is not calculated yet, need to wait to avoid crash while requesting currentUser where it's absent
        }
    }

    BackHandler { navigator.finish() }
}

@Composable
fun ImportMediaRestrictedContent(
    fileSharingRestrictedState: FeatureFlagState.SharingRestrictedState,
    importMediaAuthenticatedState: ImportMediaAuthenticatedState,
    navigateBack: () -> Unit,
) {
    with(importMediaAuthenticatedState) {
        WireScaffold(
            topBar = {
                WireCenterAlignedTopAppBar(
                    elevation = 0.dp,
                    onNavigationPressed = navigateBack,
                    navigationIconType = NavigationIconType.Close,
                    title = stringResource(id = R.string.import_media_content_title),
                    actions = {
                        UserProfileAvatar(
                            avatarData = UserAvatarData(avatarAsset),
                            clickable = remember { Clickable(enabled = false) { } }
                        )
                    }
                )
            },
            modifier = Modifier.background(colorsScheme().background),
            content = { internalPadding ->
                FileSharingRestrictedContent(
                    internalPadding,
                    fileSharingRestrictedState,
                    navigateBack
                )
            }
        )
    }
}

@Composable
fun ImportMediaRegularContent(
    importMediaAuthenticatedState: ImportMediaAuthenticatedState,
    onSearchQueryChanged: (searchQuery: TextFieldValue) -> Unit,
    onConversationClicked: (conversationId: ConversationId) -> Unit,
    checkRestrictionsAndSendImportedMedia: () -> Unit,
    onNewSelfDeletionTimerPicked: (selfDeletionDuration: SelfDeletionDuration) -> Unit,
    infoMessage: SharedFlow<SnackBarMessage>,
    navigateBack: () -> Unit,
) {

    val importMediaScreenState = rememberImportMediaScreenState()

    with(importMediaAuthenticatedState) {
        WireScaffold(
            topBar = {
                WireCenterAlignedTopAppBar(
                    elevation = 0.dp,
                    onNavigationPressed = navigateBack,
                    navigationIconType = NavigationIconType.Close,
                    title = stringResource(id = R.string.import_media_content_title),
                    actions = {
                        UserProfileAvatar(
                            avatarData = UserAvatarData(avatarAsset),
                            clickable = remember { Clickable(enabled = false) { } }
                        )
                    }
                )
            },
            modifier = Modifier.background(colorsScheme().background),
            content = { internalPadding ->
                ImportMediaContent(
                    state = this,
                    internalPadding = internalPadding,
                    onSearchQueryChanged = onSearchQueryChanged,
                    onConversationClicked = onConversationClicked,
                    searchBarState = importMediaScreenState.searchBarState
                )
            },
            bottomBar = {
                ImportMediaBottomBar(
                    state = this,
                    importMediaScreenState = importMediaScreenState,
                    checkRestrictionsAndSendImportedMedia = checkRestrictionsAndSendImportedMedia
                )
            }
        )
        MenuModalSheetLayout(
            menuItems = SelfDeletionMenuItems(
                currentlySelected = importMediaAuthenticatedState.selfDeletingTimer.duration.toSelfDeletionDuration(),
                hideEditMessageMenu = importMediaScreenState::hideBottomSheetMenu,
                onSelfDeletionDurationChanged = onNewSelfDeletionTimerPicked,
            ),
            sheetState = importMediaScreenState.bottomSheetState,
            coroutineScope = importMediaScreenState.coroutineScope
        )
    }
    SnackBarMessage(infoMessage, importMediaScreenState.snackbarHostState)
}

@Composable
fun ImportMediaLoggedOutContent(
    fileSharingRestrictedState: FeatureFlagState.SharingRestrictedState,
    navigateBack: () -> Unit,
) {
    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = 0.dp,
                onNavigationPressed = navigateBack,
                navigationIconType = NavigationIconType.Close,
                title = stringResource(id = R.string.import_media_content_title),
            )
        },
        modifier = Modifier.background(colorsScheme().background),
        content = { internalPadding ->
            FileSharingRestrictedContent(
                internalPadding,
                fileSharingRestrictedState,
                navigateBack
            )
        }
    )
}

@Composable
fun FileSharingRestrictedContent(
    internalPadding: PaddingValues,
    sharingRestrictedState: FeatureFlagState.SharingRestrictedState,
    openWireAction: () -> Unit
) {
    val context = LocalContext.current
    val learnMoreUrl = stringResource(R.string.file_sharing_restricted_learn_more_link)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(internalPadding)
            .padding(horizontal = dimensions().spacing48x)
    ) {
        val textRes =
            if (sharingRestrictedState == FeatureFlagState.SharingRestrictedState.NO_USER) {
                R.string.file_sharing_restricted_description_no_users
            } else {
                R.string.file_sharing_restricted_description_by_team
            }
        Text(
            text = stringResource(textRes),
            textAlign = TextAlign.Center,
            style = MaterialTheme.wireTypography.body01,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(dimensions().spacing16x))

        if (sharingRestrictedState == FeatureFlagState.SharingRestrictedState.NO_USER) {
            WirePrimaryButton(
                onClick = openWireAction,
                text = stringResource(R.string.file_sharing_restricted_button_text_no_users),
                fillMaxWidth = false
            )
        } else {
            LinkText(
                linkTextData = listOf(
                    LinkTextData(text = stringResource(R.string.label_learn_more),
                        tag = "learn_more",
                        annotation = learnMoreUrl,
                        onClick = { CustomTabsHelper.launchUrl(context, learnMoreUrl) }
                    )
                )
            )
        }
    }
}

@Composable
private fun ImportMediaBottomBar(
    state: ImportMediaAuthenticatedState,
    importMediaScreenState: ImportMediaScreenState,
    checkRestrictionsAndSendImportedMedia: () -> Unit,
) {
    val selfDeletionTimer = state.selfDeletingTimer
    val shortDurationLabel = selfDeletionTimer.duration.toSelfDeletionDuration().shortLabel
    val mainButtonText = if (selfDeletionTimer.duration.isPositiveNotNull()) {
        "${stringResource(id = R.string.self_deleting_message_label)} (${shortDurationLabel.asString()})"
    } else {
        stringResource(id = R.string.import_media_send_button_title)
    }
    val buttonCount =
        if (state.importedAssets.isNotEmpty() || state.importedText != null) state.selectedConversationItem.size else 0
    SendContentButton(
        mainButtonText = mainButtonText,
        count = buttonCount,
        onMainButtonClick = checkRestrictionsAndSendImportedMedia,
        selfDeletionTimer = selfDeletionTimer,
        onSelfDeletionTimerClicked = importMediaScreenState::showBottomSheetMenu,
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ImportMediaContent(
    state: ImportMediaAuthenticatedState,
    internalPadding: PaddingValues,
    onSearchQueryChanged: (searchQuery: TextFieldValue) -> Unit,
    onConversationClicked: (conversationId: ConversationId) -> Unit,
    searchBarState: SearchBarState
) {
    val importedItemsList: PersistentList<ImportedMediaAsset> = state.importedAssets
    val itemsToImport = importedItemsList.size

    val isMultipleImport = itemsToImport != 1

    Column(
        modifier = Modifier
            .padding(internalPadding)
            .fillMaxSize()
    ) {
        val lazyListState = rememberLazyListState()
        if (state.isImporting) {
            Box(
                Modifier
                    .height(dimensions().spacing100x)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
            ) {
                WireCircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    progressColor = colorsScheme().primary,
                    size = dimensions().spacing24x
                )
            }
        } else if (!isMultipleImport) {
            Box(modifier = Modifier.padding(horizontal = dimensions().spacing16x)) {
                AssetPreview(asset = importedItemsList.first(), onClick = {})
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimensions().spacing8x),
                contentPadding = PaddingValues(start = dimensions().spacing16x, end = dimensions().spacing16x)
            ) {
                items(
                    count = importedItemsList.size,
                ) { index ->
                    AssetPreview(
                        asset = importedItemsList[index],
                        onClick = {}
                    )
                }
            }
        }
        HorizontalDivider(
            color = colorsScheme().outline,
            thickness = 1.dp,
            modifier = Modifier.padding(top = dimensions().spacing12x)
        )
        Box(Modifier.padding(dimensions().spacing6x)) {
            SearchTopBar(
                isSearchActive = searchBarState.isSearchActive,
                searchBarHint = stringResource(
                    R.string.search_bar_conversations_hint,
                    stringResource(id = R.string.conversations_screen_title).lowercase()
                ),
                searchQuery = searchBarState.searchQuery,
                onSearchQueryChanged = {
                    onSearchQueryChanged(it)
                    searchBarState.searchQueryChanged(it)
                },
                onActiveChanged = searchBarState::searchActiveChanged,
            )
        }
        ConversationList(
            modifier = Modifier.weight(1f),
            lazyListState = lazyListState,
            conversationListItems = persistentMapOf(
                ConversationFolder.WithoutHeader to state.shareableConversationListState.searchResult
            ),
            conversationsAddedToGroup = state.selectedConversationItem,
            isSelectableList = true,
            onConversationSelectedOnRadioGroup = onConversationClicked,
            searchQuery = searchBarState.searchQuery.text,
            onOpenConversation = onConversationClicked,
            onEditConversation = {},
            onOpenUserProfile = {},
            onJoinCall = {},
            onPermissionPermanentlyDenied = {}
        )
    }
    BackHandler(enabled = searchBarState.isSearchActive) {
        searchBarState.closeSearch()
    }
}

@Composable
private fun SnackBarMessage(
    infoMessages: SharedFlow<SnackBarMessage>,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        infoMessages.collect { message ->
            snackbarHostState.showSnackbar(message.uiText.asString(context.resources))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewImportMediaScreenLoggedOut() {
    ImportMediaLoggedOutContent(FeatureFlagState.SharingRestrictedState.NO_USER) {}
}

@Preview(showBackground = true)
@Composable
fun PreviewImportMediaScreenRestricted() {
    ImportMediaRestrictedContent(
        FeatureFlagState.SharingRestrictedState.RESTRICTED_IN_TEAM,
        ImportMediaAuthenticatedState()
    ) {}
}

@Preview(showBackground = true)
@Composable
fun PreviewImportMediaScreenRegular() {
    ImportMediaRegularContent(
        ImportMediaAuthenticatedState(),
        {},
        {},
        {},
        {},
        MutableSharedFlow()
    ) {}
}

@Preview(showBackground = true)
@Composable
fun PreviewImportMediaBottomBar() {
    ImportMediaBottomBar(ImportMediaAuthenticatedState(), rememberImportMediaScreenState()) {}
}
