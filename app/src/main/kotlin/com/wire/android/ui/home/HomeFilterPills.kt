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
package com.wire.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.navigation.HomeDestination
import com.wire.android.ui.common.chip.WireSelectablePill
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

/**
 * Row of selectable pills that lets the user switch between the Conversations and Threads home lists.
 * The Conversations pill shows a count badge of conversations with new activity; Threads has no badge.
 */
@Composable
internal fun HomeListFilterPills(
    currentDestination: HomeDestination,
    newActivityCount: Int,
    onSelect: (HomeDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = dimensions().spacing16x, vertical = dimensions().spacing8x),
        horizontalArrangement = Arrangement.spacedBy(dimensions().spacing8x),
    ) {
        WireSelectablePill(
            label = stringResource(R.string.conversations_screen_title),
            isSelected = currentDestination == HomeDestination.Conversations,
            count = newActivityCount.takeIf { it > 0 },
            onClick = { onSelect(HomeDestination.Conversations) },
        )
        WireSelectablePill(
            label = stringResource(R.string.threads_screen_title),
            isSelected = currentDestination == HomeDestination.Threads,
            onClick = { onSelect(HomeDestination.Threads) },
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewHomeListFilterPillsConversationsSelected() = WireTheme {
    HomeListFilterPills(
        currentDestination = HomeDestination.Conversations,
        newActivityCount = 5,
        onSelect = {},
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewHomeListFilterPillsThreadsSelected() = WireTheme {
    HomeListFilterPills(
        currentDestination = HomeDestination.Threads,
        newActivityCount = 0,
        onSelect = {},
    )
}
