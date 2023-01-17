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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.search.SearchBarState
import com.wire.android.ui.common.topappbar.search.SearchTopBar
import com.wire.android.ui.common.topappbar.search.rememberSearchbarState
import com.wire.android.ui.home.conversationslist.common.ConversationList
import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.newconversation.common.SelectParticipantsButtonsRow
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.extension.getActivity
import kotlinx.collections.immutable.persistentMapOf

@Composable
fun ImportMediaScreen(importMediaViewModel: ImportMediaViewModel = hiltViewModel()) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        context.getActivity()?.let { importMediaViewModel.handleReceivedDataFromSharingIntent(it) }
    }

    val searchBarState = rememberSearchbarState()
    ImportMediaContent(searchBarState, importMediaViewModel)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class, ExperimentalFoundationApi::class)
@Composable
fun ImportMediaContent(searchBarState: SearchBarState, importMediaViewModel: ImportMediaViewModel) {
    Scaffold(topBar = {
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
    }, modifier = Modifier.background(colorsScheme().background)) { internalPadding ->
        val importedItemsList: List<ImportedMediaAsset> = importMediaViewModel.importMediaState.importedAssets
        val pagerState = rememberPagerState()
        val isMultipleImport = importedItemsList.size > 1
        val actionButtonTitle = stringResource(R.string.import_media_send_button_title)

        Column(
            modifier = Modifier
                .padding(internalPadding)
                .fillMaxSize()
        ) {
            val horizontalPadding = dimensions().spacing16x
            val screenWidth = LocalConfiguration.current.screenWidthDp.dp
            val itemWidth = if (isMultipleImport) dimensions().importedMediaAssetSize else screenWidth - (horizontalPadding * 2)
            val contentPadding = PaddingValues(start = horizontalPadding, end = (screenWidth - itemWidth + horizontalPadding))
            val lazyListState = rememberLazyListState()
            CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                HorizontalPager(
                    state = pagerState,
                    count = importedItemsList.size,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = contentPadding
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
                    onSearchQueryChanged = searchBarState::searchQueryChanged,
                    onInputClicked = searchBarState::openSearch,
                    onCloseSearchClicked = searchBarState::closeSearch
                )
                ConversationList(
                    lazyListState = lazyListState,
                    conversationListItems = persistentMapOf(
                        ConversationFolder.Predefined.Conversations to importMediaViewModel.shareableConversationListState.searchResult
                    ),
                    isSelectableList = true,
                    onConversationAddedToGroup = importMediaViewModel::addConversationToGroup,
                    onConversationRemovedFromGroup = importMediaViewModel::removeConversationFromGroup,
                    searchQuery = "",
                    onOpenConversation = {},
                    onEditConversation = {},
                    onOpenUserProfile = {},
                    onOpenConversationNotificationsSettings = {},
                    onJoinCall = {}
                )
                SelectParticipantsButtonsRow(
                    count = importMediaViewModel.shareableConversationListState.conversationsAddedToGroup.size,
                    mainButtonText = actionButtonTitle,
                    onMainButtonClick = {
//                   importMediaViewModel.onImportedMediaSent()
                    }
                )
            }
        }
        BackHandler(enabled = searchBarState.isSearchActive) {
            searchBarState.closeSearch()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewImportMediaScreen() {
    ImportMediaScreen(hiltViewModel())
}
