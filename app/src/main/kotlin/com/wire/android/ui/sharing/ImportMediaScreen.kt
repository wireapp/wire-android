package com.wire.android.ui.sharing

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsEndWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
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
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.search.SearchBarState
import com.wire.android.ui.common.topappbar.search.SearchTopBar
import com.wire.android.ui.common.topappbar.search.rememberSearchbarState
import com.wire.android.ui.home.conversationslist.common.ConversationList
import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.newconversation.common.SendContentButton
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.extension.getActivity
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.runBlocking

@Composable
fun ImportMediaScreen(importMediaViewModel: ImportMediaViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val searchBarState = rememberSearchbarState()
    val snackBarHostState: SnackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(importMediaViewModel.importMediaState) {
        if (importMediaViewModel.importMediaState.importedAssets.isEmpty()) {
            context.getActivity()?.let { importMediaViewModel.handleReceivedDataFromSharingIntent(it) }
        }
    }

    ImportMediaContent(searchBarState, snackBarHostState, importMediaViewModel)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class, ExperimentalFoundationApi::class)
@Composable
fun ImportMediaContent(searchBarState: SearchBarState, snackbarHostState: SnackbarHostState, importMediaViewModel: ImportMediaViewModel) {
    val actionButtonTitle = stringResource(R.string.import_media_send_button_title)
    Scaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = 0.dp,
                onNavigationPressed = importMediaViewModel::navigateBack,
                title = stringResource(id = R.string.import_media_content_title),
                actions = {
                    UserProfileAvatar(
                        avatarData = UserAvatarData(importMediaViewModel.importMediaState.avatarAsset),
                        clickable = remember { Clickable(enabled = false) { } }
                    )
                }
            )
        },
        snackbarHost = {
            SwipeDismissSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.fillMaxWidth()
            )
        },
        modifier = Modifier.background(colorsScheme().background),
        content = { internalPadding ->
            val importedItemsList: List<ImportedMediaAsset> = importMediaViewModel.importMediaState.importedAssets
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
                val itemWidth = if (isMultipleImport) dimensions().importedMediaAssetSize else screenWidth - (horizontalPadding * 2)
                val contentPadding = PaddingValues(start = horizontalPadding, end = (screenWidth - itemWidth + horizontalPadding))
                val lazyListState = rememberLazyListState()
                CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                    HorizontalPager(
                        state = pagerState,
                        count = itemsToImport,
                        modifier = Modifier.wrapContentWidth(),
                        contentPadding = contentPadding,
                        itemSpacing = dimensions().spacing8x
                    ) { page ->
                        ImportedMediaItemView(importedItemsList[page], isMultipleImport, importMediaViewModel.wireSessionImageLoader)
                    }
                    Divider(
                        modifier = Modifier.padding(vertical = dimensions().spacing16x),
                        color = MaterialTheme.wireColorScheme.divider,
                        thickness = Dp.Hairline
                    )
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
                    ConversationList(
                        modifier = Modifier.weight(1f),
                        lazyListState = lazyListState,
                        conversationListItems = persistentMapOf(
                            ConversationFolder.Predefined.Conversations to importMediaViewModel.shareableConversationListState.searchResult
                        ),
                        conversationsAddedToGroup = importMediaViewModel.shareableConversationListState.conversationsAddedToGroup,
                        isSelectableList = true,
                        onConversationSelectedOnRadioGroup = importMediaViewModel::selectConversationOnRadioGroup,
                        searchQuery = searchBarState.searchQuery.text,
                        onOpenConversation = importMediaViewModel::onConversationClicked,
                        onEditConversation = {},
                        onOpenUserProfile = {},
                        onOpenConversationNotificationsSettings = {},
                        onJoinCall = {}
                    )
                }
            }
            BackHandler(enabled = searchBarState.isSearchActive) {
                searchBarState.closeSearch()
            }
        },
        bottomBar = {
            SendContentButton(
                mainButtonText = actionButtonTitle,
                count = importMediaViewModel.shareableConversationListState.conversationsAddedToGroup.size,
                onMainButtonClick = importMediaViewModel::onImportedMediaSent
            )
        }
    )
    SnackBarMessage(importMediaViewModel.infoMessage, snackbarHostState)
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
