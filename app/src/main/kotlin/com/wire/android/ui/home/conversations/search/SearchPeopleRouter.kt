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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.model.ItemActionType
import com.wire.android.ui.common.CollapsingTopBarScaffold
import com.wire.android.ui.common.TabItem
import com.wire.android.ui.common.WireTabRow
import com.wire.android.ui.common.calculateCurrentTab
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.search.SearchTopBar
import com.wire.android.ui.common.topappbar.search.rememberSearchbarState
import com.wire.android.ui.home.newconversation.common.CreateNewGroupButton
import com.wire.android.ui.home.newconversation.common.SelectParticipantsButtonsAlwaysEnabled
import com.wire.android.ui.home.newconversation.common.SelectParticipantsButtonsRow
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.util.ui.UIText
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.coroutines.launch

@Suppress("ComplexMethod")
@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun SearchUsersAndServicesScreen(
    searchTitle: String,
    actionButtonTitle: String,
    selectedContacts: ImmutableSet<Contact>,
    onGroupSelectionSubmitAction: () -> Unit,
    onContactChecked: (Boolean, Contact) -> Unit,
    onOpenUserProfile: (Contact) -> Unit,
    onServiceClicked: (Contact) -> Unit,
    onClose: () -> Unit,
    screenType: SearchPeopleScreenType,
    modifier: Modifier = Modifier,
    isGroupSubmitVisible: Boolean = true,
    isServicesAllowed: Boolean = false
) {
    val searchBarState = rememberSearchbarState()
    val scope = rememberCoroutineScope()
    val initialPageIndex = SearchPeopleTabItem.PEOPLE.ordinal
    val pagerState = rememberPagerState(
        initialPage = initialPageIndex,
        pageCount = { if (isServicesAllowed) SearchPeopleTabItem.entries.size else 1 }
    )
    val currentTabState by remember { derivedStateOf { pagerState.calculateCurrentTab() } }

    CollapsingTopBarScaffold(
        modifier = modifier,
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
                        navigationIconType = when (screenType) {
                            SearchPeopleScreenType.CONVERSATION_DETAILS -> NavigationIconType.Close
                            SearchPeopleScreenType.NEW_CONVERSATION -> NavigationIconType.Close
                            SearchPeopleScreenType.NEW_GROUP_CONVERSATION -> NavigationIconType.Back
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
                searchQueryTextState = searchBarState.searchQueryTextState,
                onActiveChanged = searchBarState::searchActiveChanged,
            ) {
                if (screenType == SearchPeopleScreenType.CONVERSATION_DETAILS && isServicesAllowed) {
                    WireTabRow(
                        tabs = SearchPeopleTabItem.entries,
                        selectedTabIndex = currentTabState,
                        onTabChange = { scope.launch { pagerState.animateScrollToPage(it) } },
                        divider = {} // no divider
                    )
                }
            }
        },
        isSwipeable = !searchBarState.isSearchActive,
        content = {
            Crossfade(
                targetState = searchBarState.isSearchActive, label = ""
            ) { isSearchActive ->
                val actionType = when (screenType) {
                    SearchPeopleScreenType.NEW_CONVERSATION -> ItemActionType.CLICK
                    SearchPeopleScreenType.NEW_GROUP_CONVERSATION -> ItemActionType.CHECK
                    SearchPeopleScreenType.CONVERSATION_DETAILS -> ItemActionType.CHECK
                }

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
                                        searchQuery = searchBarState.searchQueryTextState.text.toString(),
                                        contactsAddedToGroup = selectedContacts,
                                        onOpenUserProfile = onOpenUserProfile,
                                        onContactChecked = onContactChecked,
                                        isSearchActive = isSearchActive,
                                        isLoading = false, // TODO: update correctly
                                        actionType = actionType,
                                    )
                                }

                                SearchPeopleTabItem.SERVICES -> {
                                    SearchAllServicesScreen(
                                        searchQuery = searchBarState.searchQueryTextState.text.toString(),
                                        onServiceClicked = onServiceClicked,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    SearchAllPeopleOrContactsScreen(
                        searchQuery = searchBarState.searchQueryTextState.text.toString(),
                        contactsAddedToGroup = selectedContacts,
                        onContactChecked = onContactChecked,
                        onOpenUserProfile = onOpenUserProfile,
                        isSearchActive = isSearchActive,
                        isLoading = false, // TODO: update correctly
                        actionType = actionType,
                    )
                }
            }
            BackHandler(enabled = searchBarState.isSearchActive) {
                searchBarState.closeSearch()
            }
        },
        bottomBar = {
            if (isGroupSubmitVisible) {
                when (screenType) {
                    SearchPeopleScreenType.NEW_CONVERSATION -> {
                        CreateNewGroupButton(
                                mainButtonText = actionButtonTitle,
                                onMainButtonClick = onGroupSelectionSubmitAction
                            )
                        }

                    SearchPeopleScreenType.NEW_GROUP_CONVERSATION -> {
                        SelectParticipantsButtonsAlwaysEnabled(
                            count = selectedContacts.size,
                            mainButtonText = actionButtonTitle,
                            onMainButtonClick = onGroupSelectionSubmitAction
                        )
                    }

                    SearchPeopleScreenType.CONVERSATION_DETAILS -> {
                        if (pagerState.currentPage != SearchPeopleTabItem.SERVICES.ordinal) {
                            SelectParticipantsButtonsRow(
                                selectedParticipantsCount = selectedContacts.size,
                                mainButtonText = actionButtonTitle,
                                onMainButtonClick = onGroupSelectionSubmitAction
                            )
                        }
                    }
                }
            }
        },
        snapOnFling = false,
        keepElevationWhenCollapsed = true
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
    contactsAddedToGroup: ImmutableSet<Contact>,
    isLoading: Boolean,
    isSearchActive: Boolean,
    actionType: ItemActionType,
    onOpenUserProfile: (Contact) -> Unit,
    onContactChecked: (Boolean, Contact) -> Unit,
    searchUserViewModel: SearchUserViewModel = hiltViewModel(),
) {

    LaunchedEffect(key1 = searchQuery) {
        searchUserViewModel.searchQueryChanged(searchQuery)
    }

    val lazyState = rememberLazyListState()
    SearchAllPeopleScreen(
        searchQuery = searchUserViewModel.state.searchQuery,
        contactsSearchResult = searchUserViewModel.state.contactsResult,
        publicSearchResult = searchUserViewModel.state.publicResult,
        contactsAddedToGroup = contactsAddedToGroup,
        onChecked = onContactChecked,
        onOpenUserProfile = onOpenUserProfile,
        lazyListState = lazyState,
        isSearchActive = isSearchActive,
        isLoading = isLoading,
        actionType = actionType,
    )
}
