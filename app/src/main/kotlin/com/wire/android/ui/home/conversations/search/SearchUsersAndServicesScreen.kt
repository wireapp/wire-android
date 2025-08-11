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

package com.wire.android.ui.home.conversations.search

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.model.ItemActionType
import com.wire.android.ui.common.CollapsingTopBarScaffold
import com.wire.android.ui.common.TabItem
import com.wire.android.ui.common.WireTabRow
import com.wire.android.ui.common.calculateCurrentTab
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.search.rememberSearchbarState
import com.wire.android.ui.common.topappbar.search.SearchTopBar
import com.wire.android.ui.home.newconversation.common.ContinueButton
import com.wire.android.ui.home.newconversation.common.CreateRegularGroupOrChannelButtons
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.util.ui.UIText
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.launch

@Suppress("ComplexMethod")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchUsersAndServicesScreen(
    searchTitle: String,
    selectedContacts: ImmutableSet<Contact>,
    onContactChecked: (Boolean, Contact) -> Unit,
    onOpenUserProfile: (Contact) -> Unit,
    onServiceClicked: (Contact) -> Unit,
    onClose: () -> Unit,
    screenType: SearchPeopleScreenType,
    shouldShowChannelPromotion: Boolean,
    isUserAllowedToCreateChannels: Boolean,
    modifier: Modifier = Modifier,
    isGroupSubmitVisible: Boolean = true,
    isServicesAllowed: Boolean = false,
    initialPage: SearchPeopleTabItem = SearchPeopleTabItem.PEOPLE,
    onContinue: () -> Unit = {},
    onCreateNewGroup: () -> Unit = {},
    onCreateNewChannel: () -> Unit = {}
) {
    val searchBarState = rememberSearchbarState()
    val scope = rememberCoroutineScope()
    val tabs = remember(isServicesAllowed) {
        if (isServicesAllowed) SearchPeopleTabItem.entries else listOf(SearchPeopleTabItem.PEOPLE)
    }
    val pagerState = rememberPagerState(
        initialPage = tabs.indexOf(initialPage),
        pageCount = { tabs.size }
    )
    val currentTabState by remember {
        derivedStateOf {
            pagerState.calculateCurrentTab()
        }
    }
    val lazyListStates: List<LazyListState> = List(tabs.size) {
        rememberLazyListState()
    }

    CollapsingTopBarScaffold(
        modifier = modifier,
        topBarHeader = {
            AnimatedVisibility(
                visible = !searchBarState.isSearchActive,
                enter = fadeIn() + expandVertically(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Box(modifier = Modifier.wrapContentSize()) {
                    WireCenterAlignedTopAppBar(
                        elevation = dimensions().spacing0x, // CollapsingTopBarScaffold already manages elevation
                        title = searchTitle,
                        navigationIconType = when (screenType) {
                            SearchPeopleScreenType.CONVERSATION_DETAILS ->
                                NavigationIconType.Close(R.string.content_description_add_participants_close)

                            SearchPeopleScreenType.NEW_CONVERSATION ->
                                NavigationIconType.Close(R.string.content_description_new_conversation_close_btn)

                            SearchPeopleScreenType.NEW_GROUP_CONVERSATION ->
                                NavigationIconType.Back(R.string.content_description_new_conversation_back_btn)
                        },
                        onNavigationPressed = onClose
                    )
                }
            }
        },
        topBarCollapsing = {
            SearchTopBar(
                isSearchActive = searchBarState.isSearchActive,
                searchBarHint = stringResource(R.string.label_search_people),
                searchBarDescription = stringResource(R.string.content_description_add_participants_search_field),
                searchQueryTextState = searchBarState.searchQueryTextState,
                onActiveChanged = searchBarState::searchActiveChanged,
            )
        },
        topBarFooter = {
            if (isServicesAllowed) {
                WireTabRow(
                    tabs = SearchPeopleTabItem.entries,
                    selectedTabIndex = currentTabState,
                    onTabChange = { scope.launch { pagerState.animateScrollToPage(it) } },
                )
            }
        },
        collapsingEnabled = !searchBarState.isSearchActive,
        contentLazyListState = lazyListStates[pagerState.currentPage],
        content = {
            val actionType = when (screenType) {
                SearchPeopleScreenType.NEW_CONVERSATION -> ItemActionType.CLICK
                SearchPeopleScreenType.NEW_GROUP_CONVERSATION -> ItemActionType.CHECK
                SearchPeopleScreenType.CONVERSATION_DETAILS -> ItemActionType.CHECK
            }

            CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                ) { pageIndex ->
                    when (tabs[pageIndex]) {
                        SearchPeopleTabItem.PEOPLE -> {
                            SearchAllPeopleOrContactsScreen(
                                searchQuery = searchBarState.searchQueryTextState.text.toString(),
                                contactsSelected = selectedContacts,
                                onOpenUserProfile = onOpenUserProfile,
                                onContactChecked = onContactChecked,
                                isSearchActive = searchBarState.isSearchActive,
                                actionType = actionType,
                                lazyListState = lazyListStates[pageIndex],
                            )
                        }

                        SearchPeopleTabItem.SERVICES -> {
                            SearchAllServicesScreen(
                                searchQuery = searchBarState.searchQueryTextState.text.toString(),
                                onServiceClicked = onServiceClicked,
                                lazyListState = lazyListStates[pageIndex],
                            )
                        }
                    }
                }
            }
            BackHandler(enabled = searchBarState.isSearchActive) {
                searchBarState.closeSearch()
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isGroupSubmitVisible && !(searchBarState.isSearchActive && screenType == SearchPeopleScreenType.NEW_CONVERSATION),
                enter = fadeIn() + expandVertically(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                when (screenType) {
                    SearchPeopleScreenType.NEW_CONVERSATION -> {
                        CreateRegularGroupOrChannelButtons(
                            shouldShowChannelPromotion = shouldShowChannelPromotion,
                            isUserAllowedToCreateChannels = isUserAllowedToCreateChannels,
                            onCreateNewRegularGroup = onCreateNewGroup,
                            onCreateNewChannel = onCreateNewChannel
                        )
                    }

                    SearchPeopleScreenType.NEW_GROUP_CONVERSATION -> {
                        ContinueButton(
                            onContinue = onContinue
                        )
                    }

                    SearchPeopleScreenType.CONVERSATION_DETAILS -> {
                        if (tabs[pagerState.currentPage] != SearchPeopleTabItem.SERVICES) {
                            ContinueButton(
                                onContinue = onContinue
                            )
                        }
                    }
                }
            }
        },
        snapOnFling = false,
    )
}

enum class SearchPeopleTabItem(@StringRes val titleResId: Int) : TabItem {
    PEOPLE(R.string.label_add_member_people),
    SERVICES(R.string.label_add_member_services);

    override val title: UIText = UIText.StringResource(titleResId)
}

enum class SearchPeopleScreenType {
    NEW_CONVERSATION,
    NEW_GROUP_CONVERSATION,
    CONVERSATION_DETAILS
}

@Composable
private fun SearchAllPeopleOrContactsScreen(
    searchQuery: String,
    contactsSelected: ImmutableSet<Contact>,
    isSearchActive: Boolean,
    actionType: ItemActionType,
    onOpenUserProfile: (Contact) -> Unit,
    onContactChecked: (Boolean, Contact) -> Unit,
    searchUserViewModel: SearchUserViewModel = hiltViewModel(),
    lazyListState: LazyListState = rememberLazyListState(),
) {

    LaunchedEffect(key1 = searchQuery) {
        searchUserViewModel.searchQueryChanged(searchQuery)
    }
    LaunchedEffect(key1 = contactsSelected, actionType) {
        searchUserViewModel.selectedContactsChanged(
            when (actionType) {
                ItemActionType.CLICK -> persistentSetOf() // do not pass any selected contacts in non-selectable mode
                ItemActionType.CHECK -> contactsSelected
            }
        )
    }

    var selectedContactResultsExpanded by remember { mutableStateOf(false) }
    var contactResultsExpanded by remember { mutableStateOf(true) }
    var publicResultsExpanded by remember { mutableStateOf(true) }
    SearchAllPeopleScreen(
        searchQuery = searchUserViewModel.state.searchQuery,
        contactsSearchResult = searchUserViewModel.state.contactsResult,
        publicSearchResult = searchUserViewModel.state.publicResult,
        contactsSelectedSearchResult = searchUserViewModel.state.selectedResult,
        onChecked = onContactChecked,
        onOpenUserProfile = onOpenUserProfile,
        lazyListState = lazyListState,
        isSearchActive = isSearchActive,
        isLoading = searchUserViewModel.state.isLoading,
        actionType = actionType,
        selectedContactResultsExpanded = selectedContactResultsExpanded,
        onSelectedContactResultsExpansionChanged = remember { { selectedContactResultsExpanded = it } },
        contactResultsExpanded = contactResultsExpanded,
        onContactResultsExpansionChanged = remember { { contactResultsExpanded = it } },
        publicResultsExpanded = publicResultsExpanded,
        onPublicResultsExpansionChanged = remember { { publicResultsExpanded = it } }
    )
}
