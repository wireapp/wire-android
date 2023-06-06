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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.wire.android.R
import com.wire.android.ui.common.CollapsingTopBarScaffold
import com.wire.android.ui.common.TabItem
import com.wire.android.ui.common.WireTabRow
import com.wire.android.ui.common.calculateCurrentTab
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.search.SearchTopBar
import com.wire.android.ui.common.topappbar.search.rememberSearchbarState
import com.wire.android.ui.home.conversations.details.AddMembersToConversationViewModel
import com.wire.android.ui.home.newconversation.common.SelectParticipantsButtonsAlwaysEnabled
import com.wire.android.ui.home.newconversation.common.SelectParticipantsButtonsRow
import com.wire.android.ui.home.newconversation.contacts.ContactsScreen
import com.wire.android.ui.home.newconversation.model.Contact
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.coroutines.launch

@Composable
fun AddMembersSearchRouter(
    addMembersToConversationViewModel: AddMembersToConversationViewModel = hiltViewModel()
) {
    SearchPeopleContent(
        searchPeopleState = addMembersToConversationViewModel.state,
        searchTitle = stringResource(id = R.string.label_add_participants),
        actionButtonTitle = stringResource(id = R.string.label_continue),
        onSearchQueryChanged = addMembersToConversationViewModel::searchQueryChanged,
        onOpenUserProfile = addMembersToConversationViewModel::openUserProfile,
        onAddContactToGroup = addMembersToConversationViewModel::addContactToGroup,
        onRemoveContactFromGroup = addMembersToConversationViewModel::removeContactFromGroup,
        // Members search does not have the option to add a contact
        onAddContact = { },
        onGroupSelectionSubmitAction = addMembersToConversationViewModel::addMembersToConversation,
        onClose = addMembersToConversationViewModel::close,
        onServiceClicked = addMembersToConversationViewModel::onServiceClicked,
        screenType = SearchPeopleScreenType.CONVERSATION_DETAILS
    )
}

@Composable
fun SearchPeopleRouter(
    onGroupSelectionSubmitAction: () -> Unit,
    searchAllPeopleViewModel: SearchAllPeopleViewModel,
) {
    SearchPeopleContent(
        searchPeopleState = searchAllPeopleViewModel.state,
        searchTitle = stringResource(id = R.string.label_new_conversation),
        actionButtonTitle = stringResource(id = R.string.label_new_group),
        onSearchQueryChanged = searchAllPeopleViewModel::searchQueryChanged,
        onOpenUserProfile = searchAllPeopleViewModel::openUserProfile,
        onAddContactToGroup = searchAllPeopleViewModel::addContactToGroup,
        onRemoveContactFromGroup = searchAllPeopleViewModel::removeContactFromGroup,
        onAddContact = searchAllPeopleViewModel::addContact,
        onGroupSelectionSubmitAction = onGroupSelectionSubmitAction,
        onClose = searchAllPeopleViewModel::close,
        onServiceClicked = { },
        screenType = SearchPeopleScreenType.NEW_CONVERSATION
    )
}

@OptIn(
    ExperimentalPagerApi::class,
    ExperimentalComposeUiApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun SearchPeopleContent(
    searchPeopleState: SearchPeopleState,
    searchTitle: String,
    actionButtonTitle: String,
    onSearchQueryChanged: (TextFieldValue) -> Unit,
    onGroupSelectionSubmitAction: () -> Unit,
    onAddContact: (Contact) -> Unit,
    onAddContactToGroup: (Contact) -> Unit,
    onRemoveContactFromGroup: (Contact) -> Unit,
    onOpenUserProfile: (Contact) -> Unit,
    onServiceClicked: (Contact) -> Unit,
    onClose: () -> Unit,
    screenType: SearchPeopleScreenType
) {
    val searchBarState = rememberSearchbarState()
    val scope = rememberCoroutineScope()
    val lazyListStates: List<LazyListState> = SearchPeopleTabItem.values().map { rememberLazyListState() }
    val initialPageIndex = SearchPeopleTabItem.PEOPLE.ordinal
    val pagerState = rememberPagerState(initialPage = initialPageIndex)
    val currentTabState by remember { derivedStateOf { pagerState.calculateCurrentTab() } }

    with(searchPeopleState) {
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
                val onInputClicked: () -> Unit = remember { { searchBarState.openSearch() } }
                val onCloseSearchClicked: () -> Unit = remember { { searchBarState.closeSearch() } }
                SearchTopBar(
                    isSearchActive = searchBarState.isSearchActive,
                    searchBarHint = stringResource(R.string.label_search_people),
                    searchQuery = searchQuery,
                    onSearchQueryChanged = onSearchQueryChanged,
                    onInputClicked = onInputClicked,
                    onCloseSearchClicked = onCloseSearchClicked
                ) {
                    if (screenType == SearchPeopleScreenType.CONVERSATION_DETAILS
                        && searchPeopleState.isServicesAllowed
                    ) {
                        WireTabRow(
                            tabs = SearchPeopleTabItem.values().toList(),
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
                                count = SearchPeopleTabItem.values().size,
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) { pageIndex ->
                                when (SearchPeopleTabItem.values()[pageIndex]) {
                                    SearchPeopleTabItem.PEOPLE -> {
                                        GetPeopleScreen(
                                            isSearchActive = isSearchActive,
                                            searchQuery = searchQuery.text,
                                            noneSearchSucceed = noneSearchSucceed,
                                            searchResult = searchResult,
                                            contactsAddedToGroup = contactsAddedToGroup,
                                            onAddContactToGroup = onAddContactToGroup,
                                            onRemoveContactFromGroup = onRemoveContactFromGroup,
                                            onOpenUserProfile = onOpenUserProfile,
                                            onAddContact = onAddContact,
                                            lazyListState = lazyListStates[pageIndex],
                                            initialContacts = initialContacts
                                        )
                                    }

                                    SearchPeopleTabItem.SERVICES -> SearchAllServicesScreen(
                                        searchQuery = searchQuery.text,
                                        searchResult = servicesSearchResult,
                                        initialServices = servicesInitialContacts,
                                        onServiceClicked = onServiceClicked,
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
                    } else {
                        GetPeopleScreen(
                            isSearchActive = isSearchActive,
                            searchQuery = searchQuery.text,
                            noneSearchSucceed = noneSearchSucceed,
                            searchResult = searchResult,
                            contactsAddedToGroup = contactsAddedToGroup,
                            onAddContactToGroup = onAddContactToGroup,
                            onRemoveContactFromGroup = onRemoveContactFromGroup,
                            onOpenUserProfile = onOpenUserProfile,
                            onAddContact = onAddContact,
                            initialContacts = initialContacts
                        )
                    }
                }
                BackHandler(enabled = searchBarState.isSearchActive) {
                    searchBarState.closeSearch()
                }
            },
            bottomBar = {
                if (searchPeopleState.isGroupCreationContext) {
                    SelectParticipantsButtonsAlwaysEnabled(
                        count = contactsAddedToGroup.size,
                        mainButtonText = actionButtonTitle,
                        onMainButtonClick = onGroupSelectionSubmitAction
                    )
                } else {
                    if (pagerState.currentPage != SearchPeopleTabItem.SERVICES.ordinal) {
                        SelectParticipantsButtonsRow(
                            selectedParticipantsCount = contactsAddedToGroup.size,
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
private fun GetPeopleScreen(
    isSearchActive: Boolean,
    searchQuery: String,
    noneSearchSucceed: Boolean,
    searchResult: ImmutableMap<SearchResultTitle, ContactSearchResult>,
    contactsAddedToGroup: ImmutableList<Contact>,
    onAddContactToGroup: (Contact) -> Unit,
    onRemoveContactFromGroup: (Contact) -> Unit,
    onOpenUserProfile: (Contact) -> Unit,
    onAddContact: (Contact) -> Unit,
    lazyListState: LazyListState = rememberLazyListState(),
    initialContacts: SearchResultState
) = if (isSearchActive) {
    SearchAllPeopleScreen(
        searchQuery = searchQuery,
        noneSearchSucceed = noneSearchSucceed,
        searchResult = searchResult,
        contactsAddedToGroup = contactsAddedToGroup,
        onAddToGroup = onAddContactToGroup,
        onRemoveFromGroup = onRemoveContactFromGroup,
        onOpenUserProfile = onOpenUserProfile,
        onAddContactClicked = onAddContact,
        lazyListState = lazyListState
    )
} else {
    ContactsScreen(
        allKnownContactResult = initialContacts,
        contactsAddedToGroup = contactsAddedToGroup,
        onAddToGroup = onAddContactToGroup,
        onRemoveFromGroup = onRemoveContactFromGroup,
        onOpenUserProfile = onOpenUserProfile
    )
}
