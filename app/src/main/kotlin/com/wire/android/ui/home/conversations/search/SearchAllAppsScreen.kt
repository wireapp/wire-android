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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
import com.wire.android.ui.home.conversations.search.widget.SearchFailureBox
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.sectionWithElements
import com.wire.kalium.logic.data.user.ConnectionState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Composable
fun SearchAllAppsScreen(
    searchQuery: String,
    onServiceClicked: (Contact) -> Unit,
    searchAppsViewModel: SearchAppsViewModel = hiltViewModel(),
    lazyListState: LazyListState = rememberLazyListState(),
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
            lazyListState = lazyListState
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
    lazyListState: LazyListState = rememberLazyListState()
) {
    when {
        isLoading -> CenteredCircularProgressBarIndicator()

        !isTeamAllowedToUseApps -> {
            UpgradeToGetAppsBanner()
        }

        // todo check when coming from a conversation, if the convo config has apps enabled or not.
        // todo then show right empty state: Apps are disabled in this conversation. Enable them in the conversation options...

        searchQuery.isBlank() && result.isEmpty() -> {
            EmptySearchAppsScreen(isSelfATeamAdmin = isSelfATeamAdmin)
        }

        searchQuery.isNotBlank() && result.isEmpty() ->
            SearchFailureBox(R.string.label_no_results_found)

        else -> AppsList(
            searchQuery = searchQuery,
            onServiceClicked = onServiceClicked,
            apps = result,
            lazyListState = lazyListState
        )
    }
}

@Composable
private fun EmptySearchAppsScreen(
    isSelfATeamAdmin: Boolean
) {
    val (title, description) = if (isSelfATeamAdmin) {
        Pair(
            stringResource(R.string.search_results_apps_empty_title),
            stringResource(R.string.search_results_apps_empty_description_team_admin)
        )
    } else {
        Pair(
            stringResource(R.string.search_results_apps_empty_title),
            stringResource(R.string.search_results_apps_empty_description_non_team_admin)
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(dimensions().spacing16x),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.wireTypography.body02,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(dimensions().spacing16x))
        Text(
            text = description,
            style = MaterialTheme.wireTypography.body01,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AppsList(
    searchQuery: String,
    apps: List<Contact>,
    onServiceClicked: (Contact) -> Unit,
    lazyListState: LazyListState = rememberLazyListState(),
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .weight(1f)
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
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchAllServicesScreen_Loading() = WireTheme {
    SearchAllAppsContent(
        searchQuery = "",
        result = persistentListOf(),
        isLoading = true,
        onServiceClicked = {},
        isTeamAllowedToUseApps = true,
        isSelfATeamAdmin = true
    )
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
        isSelfATeamAdmin = true
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
        isSelfATeamAdmin = true
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
        isSelfATeamAdmin = true
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
        isSelfATeamAdmin = false
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
        isSelfATeamAdmin = true
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
        isSelfATeamAdmin = true
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
