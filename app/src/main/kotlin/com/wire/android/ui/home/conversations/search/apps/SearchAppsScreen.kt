/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.search.apps

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.UserBadge
import com.wire.android.ui.common.avatar.UserProfileAvatar
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.CenteredCircularProgressBarIndicator
import com.wire.android.ui.common.rowitem.RowItemTemplate
import com.wire.android.ui.common.upgradetoapps.UpgradeToGetAppsBanner
import com.wire.android.ui.home.conversations.search.HighlightName
import com.wire.android.ui.home.conversations.search.widget.SearchFailureBox
import com.wire.android.util.debug.FeatureVisibilityFlags
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.sectionWithElements
import com.wire.kalium.logic.data.user.ConnectionState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Composable
fun SearchAppsScreen(
    searchQuery: String,
    onServiceClicked: (Contact) -> Unit,
    isConversationAppsEnabled: Boolean,
    searchAppsViewModel: SearchAppsViewModel = hiltViewModel(),
    lazyListState: LazyListState = rememberLazyListState()
) {
    LaunchedEffect(key1 = searchQuery) {
        searchAppsViewModel.searchQueryChanged(searchQuery)
    }

    with(searchAppsViewModel) {
        SearchAllAppsContent(
            searchQuery = state.searchQuery,
            onServiceClicked = onServiceClicked,
            result = state.result,
            isLoading = state.isLoading,
            isTeamAllowedToUseApps = state.isTeamAllowedToUseApps,
            isSelfATeamAdmin = state.isSelfATeamAdmin,
            lazyListState = lazyListState,
            isConversationAppsEnabled = isConversationAppsEnabled
        )
    }
}

@Composable
private fun SearchAllAppsContent(
    searchQuery: String,
    result: ImmutableList<Contact>,
    isLoading: Boolean,
    onServiceClicked: (Contact) -> Unit,
    isTeamAllowedToUseApps: Boolean,
    isSelfATeamAdmin: Boolean,
    isConversationAppsEnabled: Boolean,
    lazyListState: LazyListState = rememberLazyListState()
) {
    val appsContentState by rememberAppsContentState(
        isConversationAppsEnabled = isConversationAppsEnabled,
        isLoading = isLoading,
        isTeamAllowedToUseApps = isTeamAllowedToUseApps,
        searchQuery = searchQuery,
        result = result
    )
    // Reset scroll position only when search query changes (not on loading/state changes)
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            lazyListState.scrollToItem(0)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Crossfade(
            targetState = appsContentState,
            label = "SearchAppsContentTransition",
            modifier = Modifier.fillMaxSize()
        ) { state ->
            // order here matters for Crossfade animation and priority of states
            when (state) {
                AppsContentState.LOADING -> {
                    CenteredCircularProgressBarIndicator()
                }

                AppsContentState.APPS_NOT_ENABLED_FOR_CONVERSATION -> {
                    EmptySearchDisabledByConversationContent()
                }

                AppsContentState.TEAM_NOT_ALLOWED -> {
                    UpgradeToGetAppsBanner()
                }

                AppsContentState.EMPTY_INITIAL -> {
                    EmptySearchAppsContent(isSelfATeamAdmin = isSelfATeamAdmin)
                }

                AppsContentState.EMPTY_SEARCH -> {
                    SearchFailureBox(R.string.label_no_results_found)
                }

                AppsContentState.SHOW_RESULTS -> {
                    AppsList(
                        searchQuery = searchQuery,
                        onServiceClicked = onServiceClicked,
                        apps = result,
                        lazyListState = lazyListState
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberAppsContentState(
    isConversationAppsEnabled: Boolean,
    isLoading: Boolean,
    isTeamAllowedToUseApps: Boolean,
    searchQuery: String,
    result: ImmutableList<Contact>
): State<AppsContentState> = remember(isConversationAppsEnabled, isLoading, isTeamAllowedToUseApps, searchQuery, result) {
    derivedStateOf {
        // WPB-21835: Apps availability checks controlled by feature flag
        if (!FeatureVisibilityFlags.AppsBasedOnProtocol) {
            // new logic: check team and conversation settings first
            if (!isTeamAllowedToUseApps) return@derivedStateOf AppsContentState.TEAM_NOT_ALLOWED
            if (!isConversationAppsEnabled) return@derivedStateOf AppsContentState.APPS_NOT_ENABLED_FOR_CONVERSATION
        }
        // current logic: protocol-based, skip the above checks (screen shouldn't be accessible if apps disabled)

        when {
            isLoading -> AppsContentState.LOADING
            searchQuery.isBlank() && result.isEmpty() -> AppsContentState.EMPTY_SEARCH
            searchQuery.isNotBlank() && result.isEmpty() -> AppsContentState.EMPTY_SEARCH
            else -> AppsContentState.SHOW_RESULTS
        }
    }
}

@Composable
private fun AppsList(
    searchQuery: String,
    apps: List<Contact>,
    onServiceClicked: (Contact) -> Unit,
    lazyListState: LazyListState = rememberLazyListState(),
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier

    ) {
        sectionWithElements(
            items = apps.associateBy { it.id }
        ) {
            val clickDescription = stringResource(id = R.string.content_description_open_service_label)
            RowItemTemplate(
                leadingIcon = {
                    Row {
                        UserProfileAvatar(it.avatarData)
                    }
                },
                titleStartPadding = dimensions().spacing0x,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HighlightName(
                            name = it.name,
                            searchQuery = searchQuery,
                            modifier = Modifier.weight(weight = 1f, fill = false)
                        )
                        UserBadge(
                            membership = it.membership,
                            connectionState = it.connectionState,
                            startPadding = dimensions().spacing8x
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .wrapContentWidth()
                            .padding(end = dimensions().spacing4x)
                    ) {
                        ArrowRightIcon(Modifier.align(Alignment.TopEnd), R.string.content_description_empty)
                    }
                },
                clickable = remember(it) { Clickable(onClickDescription = clickDescription) { onServiceClicked(it) } },
                modifier = Modifier.padding(start = dimensions().spacing8x)
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchAllServicesScreen_TeamNotEnabledForApps() = WireTheme {
    SearchAllAppsContent(
        searchQuery = "",
        result = persistentListOf(),
        isLoading = false,
        onServiceClicked = {},
        isTeamAllowedToUseApps = false,
        isSelfATeamAdmin = true,
        isConversationAppsEnabled = true
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchAllServicesScreen_InitialResults() = WireTheme {
    SearchAllAppsContent(
        searchQuery = "",
        result = previewServiceList(count = 10).toPersistentList(),
        isLoading = false,
        onServiceClicked = {},
        isTeamAllowedToUseApps = true,
        isSelfATeamAdmin = true,
        isConversationAppsEnabled = true
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchAllServicesScreen_EmptyInitialResults_TeamAdmin() = WireTheme {
    SearchAllAppsContent(
        searchQuery = "",
        result = persistentListOf(),
        isLoading = false,
        onServiceClicked = {},
        isTeamAllowedToUseApps = true,
        isSelfATeamAdmin = true,
        isConversationAppsEnabled = true
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchAllServicesScreen_EmptyInitialResults_NonTeamAdmin() = WireTheme {
    SearchAllAppsContent(
        searchQuery = "",
        result = persistentListOf(),
        isLoading = false,
        onServiceClicked = {},
        isTeamAllowedToUseApps = true,
        isSelfATeamAdmin = false,
        isConversationAppsEnabled = true
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchAllServicesScreen_SearchResults() = WireTheme {
    SearchAllAppsContent(
        searchQuery = "Serv",
        result = previewServiceList(count = 10).toPersistentList(),
        isLoading = false,
        onServiceClicked = {},
        isTeamAllowedToUseApps = true,
        isSelfATeamAdmin = true,
        isConversationAppsEnabled = true
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchAllServicesScreen_EmptySearchResults() = WireTheme {
    SearchAllAppsContent(
        searchQuery = "Serv",
        result = persistentListOf(),
        isLoading = false,
        onServiceClicked = {},
        isTeamAllowedToUseApps = true,
        isSelfATeamAdmin = true,
        isConversationAppsEnabled = true
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchAllServicesScreen_EmptySearchResultsDisabledInConversation() = WireTheme {
    SearchAllAppsContent(
        searchQuery = "Serv",
        result = persistentListOf(),
        isLoading = false,
        onServiceClicked = {},
        isTeamAllowedToUseApps = true,
        isSelfATeamAdmin = true,
        isConversationAppsEnabled = false
    )
}

private fun previewService(index: Int) = Contact(
    id = index.toString(),
    domain = "wire.com",
    name = "Service nr $index",
    handle = "service_$index",
    connectionState = ConnectionState.NOT_CONNECTED,
    membership = Membership.Service,
)

private fun previewServiceList(count: Int): List<Contact> = buildList {
    repeat(count) { index -> add(previewService(index)) }
}
