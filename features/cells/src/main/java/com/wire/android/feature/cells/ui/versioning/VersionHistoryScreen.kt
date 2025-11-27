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
package com.wire.android.feature.cells.ui.versioning

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.feature.cells.R
import com.wire.android.navigation.WireNavigator
import com.wire.android.navigation.annotation.features.cells.WireDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.divider.WireDivider
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme

@WireDestination(
    style = PopUpNavigationAnimation::class,
    navArgsDelegate = VersionHistoryNavArgs::class,
)
@Composable
fun VersionHistoryScreen(
    navigator: WireNavigator,
    modifier: Modifier = Modifier,
    versionHistoryViewModel: VersionHistoryViewModel = hiltViewModel()
) {
    VersionHistoryScreenContent(
        versionsGroupedByTime = versionHistoryViewModel.versionsGroupedByTime.value,
        modifier = modifier,
        navigateBack = { navigator.navigateBack() }
    )
}

@Composable
private fun VersionHistoryScreenContent(
    versionsGroupedByTime: List<VersionGroup>,
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit = {}
) {
    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                onNavigationPressed = navigateBack,
                title = stringResource(R.string.version_history_top_appbar_title),
                navigationIconType = NavigationIconType.Close(),
                elevation = dimensions().spacing0x,
            )
        },
    ) { innerPadding ->
        LazyColumn(Modifier.padding(innerPadding)) {
            versionsGroupedByTime.forEach { group ->
                item {
                    VersionTimeHeaderItem(group.dateLabel)
                }
                group.uiItems.forEach {
                    item {
                        VersionItem(
                            modifiedAt = it.modifiedAt,
                            modifiedBy = it.modifiedBy,
                            fileSize = it.fileSize,
                        )
                        WireDivider(
                            modifier = Modifier.fillMaxWidth(),
                            color = colorsScheme().outline
                        )
                    }
                }
            }
        }
    }
}

@MultipleThemePreviews
@Composable
fun PreviewVersionHistoryScreenContent() {
    WireTheme {
        VersionHistoryScreenContent(
            versionsGroupedByTime = listOf(
                VersionGroup(
                    dateLabel = "Today, 3 Dec 2025",
                    uiItems = listOf(
                        CellVersion("1:46 PM", "Deniz Agha", "200MB"),
                        CellVersion("11:20 AM", "Alice Smith", "150MB"),
                        CellVersion("09:15 AM", "John Doe", "100KB"),
                        CellVersion("08:00 AM", "Eve Davis", "340KB"),
                        CellVersion("07:30 AM", "Frank Miller", "1GB"),
                    )
                ),
                VersionGroup(
                    dateLabel = "1 Dec 2025",
                    uiItems = listOf(
                        CellVersion("3:15 PM", "Bob Johnson", "300MB"),
                        CellVersion("10:05 AM", "Charlie Brown", "250KB"),
                    )
                )
            )
        )
    }
}
