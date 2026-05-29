/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.ui.debug.cryptostats

import com.wire.android.navigation.annotation.app.WireRootDestination
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.di.wireViewModel
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.ui.common.SearchBarInput
import com.wire.android.ui.common.chip.WireFilterChip
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.WireTopAppBarTitle
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@WireRootDestination
@Composable
fun ConversationCryptoStatsScreen(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: ConversationCryptoStatsViewModel = wireViewModel(),
) {
    val scrollState = rememberScrollState()
    val state by viewModel.state.collectAsState()

    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = scrollState.rememberTopBarElevationState().value,
                titleContent = {
                    WireTopAppBarTitle(
                        title = stringResource(R.string.debug_settings_conversation_crypto_stats),
                        style = typography().title01,
                        maxLines = 2
                    )
                },
                navigationIconType = NavigationIconType.Close(R.string.content_description_conversation_details_close_btn),
                onNavigationPressed = navigator::navigateBack,
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState),
            ) {
                when {
                    state.isLoading -> {
                        Text(
                            text = "Loading...",
                            modifier = Modifier.padding(dimensions().spacing16x),
                            style = MaterialTheme.wireTypography.body01,
                        )
                    }
                    state.error != null -> {
                        Text(
                            text = "Error: ${state.error}",
                            modifier = Modifier.padding(dimensions().spacing16x),
                            style = MaterialTheme.wireTypography.body01,
                            color = MaterialTheme.wireColorScheme.error,
                        )
                    }
                    state.stats != null -> {
                        val uiModel = state.stats!!.toUiModel()
                        StatsSummary(uiModel = uiModel)
                        Spacer(modifier = Modifier.height(dimensions().spacing8x))
                        FilterSection(state = state, viewModel = viewModel)
                        Spacer(modifier = Modifier.height(dimensions().spacing8x))
                        val filtered = viewModel.filteredDetails()
                        ConversationDetailsList(
                            details = filtered,
                            totalCount = uiModel.details.size,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun StatsSummary(uiModel: ConversationCryptoStatsUiModel) {
    Column(modifier = Modifier.padding(horizontal = dimensions().spacing16x)) {
        Text(
            text = "Summary",
            style = MaterialTheme.wireTypography.title02,
        )
        Spacer(modifier = Modifier.height(dimensions().spacing8x))
        StatRow("Total conversations", uiModel.totalConversations.toString())
        StatRow("Proteus", uiModel.proteusCount.toString())
        StatRow("MLS", uiModel.mlsCount.toString())
        StatRow("Mixed", uiModel.mixedCount.toString())
        HorizontalDivider(modifier = Modifier.padding(vertical = dimensions().spacing4x))
        StatRow("MLS drift", uiModel.mlsDriftCount.toString())
        StatRow("Mixed drift", uiModel.mixedDriftCount.toString())
        StatRow("MLS left", uiModel.mlsLeftCount.toString())
        StatRow("Mixed left", uiModel.mixedLeftCount.toString())
        StatRow("CC lookup failed", uiModel.ccLookupFailedCount.toString())
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(
    state: ConversationCryptoStatsViewState,
    viewModel: ConversationCryptoStatsViewModel,
) {
    Column(modifier = Modifier.padding(horizontal = dimensions().spacing16x)) {
        SearchBarInput(
            placeholderText = "Search conversations...",
            leadingIcon = {
                Text(
                    "\uD83D\uDD0D",
                    style = MaterialTheme.wireTypography.body01,
                )
            },
            textState = state.searchQuery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = dimensions().spacing4x),
        )
        Spacer(modifier = Modifier.height(dimensions().spacing4x))
        Text(
            text = "Protocol",
            style = MaterialTheme.wireTypography.label01,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(dimensions().spacing4x),
            verticalArrangement = Arrangement.spacedBy(dimensions().spacing4x),
            modifier = Modifier.padding(vertical = dimensions().spacing4x),
        ) {
            ProtocolFilter.entries.forEach { filter ->
                WireFilterChip(
                    label = filter.label,
                    isSelected = state.protocolFilter == filter,
                    onClick = { viewModel.setProtocolFilter(filter) },
                )
            }
        }
        Text(
            text = "Crypto status",
            style = MaterialTheme.wireTypography.label01,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(dimensions().spacing4x),
            verticalArrangement = Arrangement.spacedBy(dimensions().spacing4x),
            modifier = Modifier.padding(vertical = dimensions().spacing4x),
        ) {
            EstablishmentFilter.entries.forEach { filter ->
                WireFilterChip(
                    label = filter.label,
                    isSelected = state.establishmentFilter == filter,
                    onClick = { viewModel.setEstablishmentFilter(filter) },
                )
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimensions().spacing2x),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.wireTypography.body01,
        )
        Text(
            text = value,
            style = MaterialTheme.wireTypography.body02,
        )
    }
}

@Composable
private fun ConversationDetailsList(
    details: List<ConversationCryptoDetailUiModel>,
    totalCount: Int,
) {
    Column(modifier = Modifier.padding(horizontal = dimensions().spacing16x)) {
        Text(
            text = if (details.size == totalCount) {
                "Conversations ($totalCount)"
            } else {
                "Showing ${details.size} of $totalCount"
            },
            style = MaterialTheme.wireTypography.title02,
        )
        Spacer(modifier = Modifier.height(dimensions().spacing8x))
        if (details.isEmpty()) {
            Text(
                text = "No conversations match the current filters",
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.onSecondaryButtonDisabled,
            )
        }
        details.forEach { detail ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = dimensions().spacing4x),
            ) {
                Text(
                    text = detail.conversationName,
                    style = MaterialTheme.wireTypography.body02,
                )
                Text(
                    text = "Protocol: ${detail.protocolType}",
                    style = MaterialTheme.wireTypography.body01,
                )
                if (detail.groupId != null) {
                    Text(
                        text = "Group ID: ${detail.groupId}",
                        style = MaterialTheme.wireTypography.body01,
                    )
                }
                if (detail.dbGroupState != null) {
                    Text(
                        text = "DB state: ${detail.dbGroupState}",
                        style = MaterialTheme.wireTypography.body01,
                    )
                }
                if (detail.dbEpoch != null) {
                    Text(
                        text = "DB epoch: ${detail.dbEpoch}",
                        style = MaterialTheme.wireTypography.body01,
                    )
                }
                if (detail.ccEpoch != null) {
                    Text(
                        text = "CC epoch: ${detail.ccEpoch}",
                        style = MaterialTheme.wireTypography.body01,
                    )
                }
                Text(
                    text = "Self is member: ${detail.selfIsMember}",
                    style = MaterialTheme.wireTypography.body01,
                )
                Text(
                    text = "CC lookup failed: ${detail.ccLookupFailed}",
                    style = MaterialTheme.wireTypography.body01,
                )
                val color = when {
                    detail.cryptoStatus == ConversationCryptoStatus.IN_SYNC -> MaterialTheme.wireColorScheme.positive
                    detail.cryptoStatus == ConversationCryptoStatus.NOT_APPLICABLE -> MaterialTheme.wireColorScheme.onBackground
                    else -> MaterialTheme.wireColorScheme.onBackground
                }
                Text(
                    text = "Status: ${detail.cryptoStatus.label}",
                    style = MaterialTheme.wireTypography.body01,
                    color = color,
                )
            }
            HorizontalDivider()
        }
    }
}
