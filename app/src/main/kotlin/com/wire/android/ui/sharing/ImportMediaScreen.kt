package com.wire.android.ui.sharing

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.model.SnackBarMessage
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.bottomsheet.MenuModalSheetLayout
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.search.SearchBarState
import com.wire.android.ui.common.topappbar.search.SearchTopBar
import com.wire.android.ui.home.FeatureFlagState
import com.wire.android.ui.home.conversations.selfdeletion.SelfDeletionMapper.toSelfDeletionDuration
import com.wire.android.ui.home.conversations.selfdeletion.SelfDeletionMenuItems
import com.wire.android.ui.home.conversationslist.common.ConversationList
import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.newconversation.common.SendContentButton
import com.wire.android.ui.home.sync.FeatureFlagNotificationViewModel
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.extension.getActivity
import com.wire.android.util.ui.LinkText
import com.wire.android.util.ui.LinkTextData
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.SharedFlow
import kotlin.time.Duration

@Composable
fun ImportMediaScreen(
    importMediaViewModel: ImportMediaUnauthenticatedViewModel = hiltViewModel(),
    featureFlagNotificationViewModel: FeatureFlagNotificationViewModel = hiltViewModel()
) {
    featureFlagNotificationViewModel.loadInitialSync()

    ImportMediaContent(importMediaViewModel, featureFlagNotificationViewModel)
}

@Composable
fun ImportMediaContent(
    unauthorizedViewModel: ImportMediaUnauthenticatedViewModel,
    featureFlagNotificationViewModel: FeatureFlagNotificationViewModel
) {

    when (val state = featureFlagNotificationViewModel.featureFlagState.fileSharingRestrictedState) {
        FeatureFlagState.SharingRestrictedState.NO_USER -> ImportMediaLoggedOutContent(
            fileSharingRestrictedState = state,
            viewModel = unauthorizedViewModel
        )

        FeatureFlagState.SharingRestrictedState.RESTRICTED_IN_TEAM -> ImportMediaRestrictedContent(state)
        FeatureFlagState.SharingRestrictedState.NONE -> ImportMediaRegularContent()
        null -> {
            // state is not calculated yet, need to wait to avoid crash while requesting currentUser where it's absent
        }
    }

    BackHandler { unauthorizedViewModel.navigateBack() }
}

@Composable
fun ImportMediaRestrictedContent(
    fileSharingRestrictedState: FeatureFlagState.SharingRestrictedState,
    importMediaViewModel: ImportMediaAuthenticatedViewModel = hiltViewModel()
) {
    with(importMediaViewModel.importMediaState) {
        Scaffold(
            topBar = {
                WireCenterAlignedTopAppBar(
                    elevation = 0.dp,
                    onNavigationPressed = importMediaViewModel::navigateBack,
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
                    importMediaViewModel::navigateBack
                )
            }
        )
    }
}

@Composable
fun ImportMediaRegularContent(authorizedViewModel: ImportMediaAuthenticatedViewModel = hiltViewModel()) {
    val context = LocalContext.current
    LaunchedEffect(authorizedViewModel.importMediaState.importedAssets) {
        if (authorizedViewModel.importMediaState.importedAssets.isEmpty()) {
            context.getActivity()?.let { authorizedViewModel.handleReceivedDataFromSharingIntent(it) }
        }
    }
    val importMediaScreenState = rememberImportMediaScreenState()

    with(authorizedViewModel.importMediaState) {
        Scaffold(
            topBar = {
                WireCenterAlignedTopAppBar(
                    elevation = 0.dp,
                    onNavigationPressed = authorizedViewModel::navigateBack,
                    title = stringResource(id = R.string.import_media_content_title),
                    actions = {
                        UserProfileAvatar(
                            avatarData = UserAvatarData(avatarAsset),
                            clickable = remember { Clickable(enabled = false) { } }
                        )
                    }
                )
            },
            snackbarHost = {
                SwipeDismissSnackbarHost(
                    hostState = importMediaScreenState.snackbarHostState,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            modifier = Modifier.background(colorsScheme().background),
            content = { internalPadding ->
                ImportMediaContent(this, internalPadding, authorizedViewModel, importMediaScreenState.searchBarState)
            },
            bottomBar = { ImportMediaBottomBar(authorizedViewModel, importMediaScreenState) }
        )
        MenuModalSheetLayout(
            menuItems = SelfDeletionMenuItems(
                currentlySelected = authorizedViewModel.importMediaState.selfDeletingTimer.toDuration().toSelfDeletionDuration(),
                hideEditMessageMenu = importMediaScreenState::hideBottomSheetMenu,
                onSelfDeletionDurationChanged = authorizedViewModel::onNewSelfDeletionTimerPicked,
            ),
            sheetState = importMediaScreenState.bottomSheetState,
            coroutineScope = importMediaScreenState.coroutineScope
        )
    }
    SnackBarMessage(authorizedViewModel.infoMessage, importMediaScreenState.snackbarHostState)
}

@Composable
fun ImportMediaLoggedOutContent(
    viewModel: ImportMediaUnauthenticatedViewModel,
    fileSharingRestrictedState: FeatureFlagState.SharingRestrictedState
) {
    Scaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = 0.dp,
                onNavigationPressed = viewModel::navigateBack,
                title = stringResource(id = R.string.import_media_content_title),
            )
        },
        modifier = Modifier.background(colorsScheme().background),
        content = { internalPadding ->
            FileSharingRestrictedContent(
                internalPadding,
                fileSharingRestrictedState,
                viewModel::navigateBack
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
        val textRes = if (sharingRestrictedState == FeatureFlagState.SharingRestrictedState.NO_USER) {
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
    importMediaViewModel: ImportMediaAuthenticatedViewModel,
    importMediaScreenState: ImportMediaScreenState
) {
    val selfDeletionTimer = importMediaViewModel.importMediaState.selfDeletingTimer
    val shortDurationLabel = selfDeletionTimer.toDuration().toSelfDeletionDuration().shortLabel
    val mainButtonText = if (selfDeletionTimer.toDuration() > Duration.ZERO) {
        "${stringResource(id = R.string.self_deleting_message_label)} (${shortDurationLabel.asString()})"
    } else {
        stringResource(id = R.string.import_media_send_button_title)
    }
    SendContentButton(
        mainButtonText = mainButtonText,
        count = importMediaViewModel.currentSelectedConversationsCount(),
        onMainButtonClick = importMediaViewModel::checkRestrictionsAndSendImportedMedia,
        selfDeletionTimer = selfDeletionTimer,
        onSelfDeletionTimerClicked = importMediaScreenState::showBottomSheetMenu,
    )
}

@Composable
@OptIn(ExperimentalPagerApi::class, ExperimentalFoundationApi::class)
private fun ImportMediaContent(
    state: ImportMediaState,
    internalPadding: PaddingValues,
    importMediaViewModel: ImportMediaAuthenticatedViewModel,
    searchBarState: SearchBarState
) {
    val importedItemsList: List<ImportedMediaAsset> = state.importedAssets
    val itemsToImport = importedItemsList.size
    val pagerState = rememberPagerState()
    val isMultipleImport = itemsToImport > 1

    Column(
        modifier = Modifier
            .padding(internalPadding)
            .fillMaxSize()
    ) {
        val horizontalPadding = dimensions().spacing8x
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
        val itemWidth = if (isMultipleImport) dimensions().importedMediaAssetSize + horizontalPadding.times(2)
        else screenWidth - (horizontalPadding * 2)
        val contentPadding = PaddingValues(start = horizontalPadding, end = (screenWidth - itemWidth + horizontalPadding))
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
        } else {
            CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                HorizontalPager(
                    state = pagerState,
                    count = itemsToImport,
                    contentPadding = contentPadding,
                    itemSpacing = dimensions().spacing8x
                ) { page ->
                    ImportedMediaItemView(
                        importedItemsList[page],
                        isMultipleImport,
                        importMediaViewModel.wireSessionImageLoader
                    )
                }
            }
        }
        Divider(color = colorsScheme().outline, thickness = 1.dp, modifier = Modifier.padding(top = dimensions().spacing12x))
        Box(Modifier.padding(dimensions().spacing6x)) {
            SearchTopBar(
                isSearchActive = searchBarState.isSearchActive,
                searchBarHint = stringResource(
                    R.string.search_bar_conversations_hint,
                    stringResource(id = R.string.conversations_screen_title).lowercase()
                ),
                searchQuery = searchBarState.searchQuery,
                onSearchQueryChanged = {
                    importMediaViewModel.onSearchQueryChanged(it)
                    searchBarState.searchQueryChanged(it)
                },
                onInputClicked = searchBarState::openSearch,
                onCloseSearchClicked = searchBarState::closeSearch
            )
        }
        ConversationList(
            modifier = Modifier.weight(1f),
            lazyListState = lazyListState,
            conversationListItems = persistentMapOf(
                ConversationFolder.Predefined.Conversations to state.shareableConversationListState.searchResult
            ),
            conversationsAddedToGroup = state.selectedConversationItem,
            isSelectableList = true,
            onConversationSelectedOnRadioGroup = importMediaViewModel::onConversationClicked,
            searchQuery = searchBarState.searchQuery.text,
            onOpenConversation = importMediaViewModel::onConversationClicked,
            onEditConversation = {},
            onOpenUserProfile = {},
            onOpenConversationNotificationsSettings = {},
            onJoinCall = {}
        )
    }
    BackHandler(enabled = searchBarState.isSearchActive) {
        searchBarState.closeSearch()
    }
}

@Composable
private fun SnackBarMessage(infoMessages: SharedFlow<SnackBarMessage>, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        infoMessages.collect { message ->
            snackbarHostState.showSnackbar(message.uiText.asString(context.resources))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewImportMediaScreen() {
    ImportMediaScreen(hiltViewModel())
}

@Preview(showBackground = true)
@Composable
fun PreviewImportMediaBottomBar() {
    ImportMediaBottomBar(hiltViewModel(), rememberImportMediaScreenState())
}
