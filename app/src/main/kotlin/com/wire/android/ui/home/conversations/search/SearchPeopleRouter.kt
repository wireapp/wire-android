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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.ui.common.CollapsingTopBarScaffold
import com.wire.android.ui.common.TabItem
import com.wire.android.ui.common.WireTabRow
import com.wire.android.ui.common.calculateCurrentTab
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.search.SearchTopBar
import com.wire.android.ui.common.topappbar.search.rememberSearchbarState
import com.wire.android.ui.home.newconversation.common.SelectParticipantsButtonsAlwaysEnabled
import com.wire.android.ui.home.newconversation.common.SelectParticipantsButtonsRow
import com.wire.android.ui.home.newconversation.model.Contact
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.coroutines.launch

@Suppress("ComplexMethod")
@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun SearchUsersAndServicesScreen(
    searchState: SearchState,
    searchTitle: String,
    actionButtonTitle: String,
    userSearchSignal: State<String>,
    serviceSearchSignal: State<String>,
    selectedContacts: ImmutableSet<Contact>,
    onServicesSearchQueryChanged: (TextFieldValue) -> Unit,
    onUsersSearchQueryChanged: (TextFieldValue) -> Unit,
    onGroupSelectionSubmitAction: () -> Unit,
    onContactChecked: (Boolean, Contact) -> Unit,
    onOpenUserProfile: (Contact) -> Unit,
    onServiceClicked: (Contact) -> Unit,
    onClose: () -> Unit,
    screenType: SearchPeopleScreenType
) {
    val searchBarState = rememberSearchbarState()
    val scope = rememberCoroutineScope()
    val initialPageIndex = SearchPeopleTabItem.PEOPLE.ordinal
    val pagerState = rememberPagerState(
        initialPage = initialPageIndex,
        pageCount = { if (searchState.isServicesAllowed) SearchPeopleTabItem.entries.size else 1 })
    val currentTabState by remember { derivedStateOf { pagerState.calculateCurrentTab() } }

    CollapsingTopBarScaffold(
        topBarHeader = { elevation ->
            AnimatedVisibility(
                visible = !searchBarState.isSearchActive,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Box(modifier = Modifier.wrapContentSize()) {
                    WireCenterAlignedTopAppBar(
                        elevation = elevation,
                        title = searchTitle,
                        navigationIconType = NavigationIconType.Close,
                        onNavigationPressed = onClose
                    )
                }
            }
        },
        topBarCollapsing = {
            val query = when (currentTabState) {
                SearchPeopleTabItem.PEOPLE.ordinal -> searchState.userSearchQuery
                SearchPeopleTabItem.SERVICES.ordinal -> searchState.serviceSearchQuery
                else -> error("Unknown tab index $currentTabState")
            }

            val onQueryChanged: (TextFieldValue) -> Unit = when (currentTabState) {
                SearchPeopleTabItem.PEOPLE.ordinal -> onUsersSearchQueryChanged
                SearchPeopleTabItem.SERVICES.ordinal -> onServicesSearchQueryChanged
                else -> error("Unknown tab index $currentTabState")
            }

            SearchTopBar(
                isSearchActive = searchBarState.isSearchActive,
                searchBarHint = stringResource(R.string.label_search_people),
                searchQuery = query,
                onSearchQueryChanged = onQueryChanged,
                onActiveChanged = searchBarState::searchActiveChanged,
            ) {
                if (screenType == SearchPeopleScreenType.CONVERSATION_DETAILS
                    && searchState.isServicesAllowed
                ) {
                    WireTabRow(
                        tabs = SearchPeopleTabItem.entries,
                        selectedTabIndex = currentTabState,
                        onTabChange = { scope.launch { pagerState.animateScrollToPage(it) } },
                        divider = {} // no divider
                    )
                }
            }
        },
        content = {
            Crossfade(
                targetState = searchBarState.isSearchActive, label = ""
            ) { isSearchActive ->
                var focusedTabIndex: Int by remember { mutableStateOf(initialPageIndex) }
                val keyboardController = LocalSoftwareKeyboardController.current
                val focusManager = LocalFocusManager.current

                if (screenType == SearchPeopleScreenType.CONVERSATION_DETAILS) {
                    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) { pageIndex ->
                            when (SearchPeopleTabItem.entries[pageIndex]) {
                                SearchPeopleTabItem.PEOPLE -> {
                                    SearchAllPeopleOrContactsScreen(
                                        searchQuery = userSearchSignal.value,
                                        contactsAddedToGroup = selectedContacts,
                                        onOpenUserProfile = onOpenUserProfile,
                                        onContactChecked = onContactChecked,
                                        isSearchActive = isSearchActive,
                                        isLoading = false // TODO: update correctly
                                    )
                                }

                                SearchPeopleTabItem.SERVICES -> {
                                    SearchAllServicesScreen(
                                        searchQuery = serviceSearchSignal.value,
                                        onServiceClicked = onServiceClicked,
                                    )
                                }
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
                } else {
                    SearchAllPeopleOrContactsScreen(
                        searchQuery = userSearchSignal.value,
                        contactsAddedToGroup = selectedContacts,
                        onContactChecked = onContactChecked,
                        onOpenUserProfile = onOpenUserProfile,
                        isSearchActive = isSearchActive,
                        isLoading = false // TODO: update correctly
                    )
                }
            }
            BackHandler(enabled = searchBarState.isSearchActive) {
                searchBarState.closeSearch()
            }
        },
        bottomBar = {
            if (searchState.isGroupCreationContext) {
                SelectParticipantsButtonsAlwaysEnabled(
                    count = selectedContacts.size,
                    mainButtonText = actionButtonTitle,
                    onMainButtonClick = onGroupSelectionSubmitAction
                )
            } else {
                if (pagerState.currentPage != SearchPeopleTabItem.SERVICES.ordinal) {
                    SelectParticipantsButtonsRow(
                        selectedParticipantsCount = selectedContacts.size,
                        mainButtonText = actionButtonTitle,
                        onMainButtonClick = onGroupSelectionSubmitAction
                    )
                }
            }
        },
        snapOnFling = false,
        keepElevationWhenCollapsed = true
    )
}

enum class SearchPeopleTabItem(@StringRes override val titleResId: Int) : TabItem {
    PEOPLE(R.string.label_add_member_people),
    SERVICES(R.string.label_add_member_services);
}

enum class SearchPeopleScreenType {
    NEW_CONVERSATION,
    CONVERSATION_DETAILS
}

@Composable
private fun SearchAllPeopleOrContactsScreen(
    searchQuery: String,
    contactsAddedToGroup: ImmutableSet<Contact>,
    isLoading: Boolean,
    isSearchActive: Boolean,
    onOpenUserProfile: (Contact) -> Unit,
    onContactChecked: (Boolean, Contact) -> Unit,
    searchUserViewModel: SearchUserViewModel = hiltViewModel(),
) {

    LaunchedEffect(key1 = searchQuery) {
        searchUserViewModel.search(searchQuery)
    }

    val lazyState = rememberLazyListState()
    SearchAllPeopleScreen(
        searchQuery = searchQuery,
        noneSearchSucceed = searchUserViewModel.state.noneSearchSucceeded,
        contactsSearchResult = searchUserViewModel.state.contactsResult,
        publicSearchResult = searchUserViewModel.state.publicResult,
        contactsAddedToGroup = contactsAddedToGroup,
        onChecked = onContactChecked,
        onOpenUserProfile = onOpenUserProfile,
        lazyListState = lazyState,
        isSearchActive = isSearchActive,
        isLoading = isLoading
    )
}
