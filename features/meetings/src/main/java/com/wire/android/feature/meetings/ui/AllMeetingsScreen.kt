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
package com.wire.android.feature.meetings.ui

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.wire.android.feature.meetings.R
import com.wire.android.feature.meetings.ui.util.PreviewMultipleThemes
import com.wire.android.ui.common.TabItem
import com.wire.android.ui.common.WireTabRow
import com.wire.android.ui.common.bottomsheet.WireBottomSheetDefaults
import com.wire.android.ui.common.calculateCurrentTab
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.rememberLazyListStateProvider
import kotlinx.coroutines.launch

@SuppressLint("ComposeModifierMissing")
@Composable
fun AllMeetingsScreen() {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        val scope = rememberCoroutineScope()
        val pagerState = rememberPagerState { MeetingsTabItem.entries.size }
        val lazyListStateProvider = rememberLazyListStateProvider<MeetingsTabItem>()

        Surface(
            color = WireBottomSheetDefaults.WireSheetContainerColor,
            shadowElevation = lazyListStateProvider[MeetingsTabItem.entries[pagerState.currentPage]].rememberTopBarElevationState().value,
            modifier = Modifier.fillMaxWidth()
        ) {
            WireTabRow(
                tabs = MeetingsTabItem.entries,
                selectedTabIndex = pagerState.calculateCurrentTab(),
                onTabChange = {
                    scope.launch {
                        pagerState.animateScrollToPage(it)
                    }
                }
            )
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) {
            val lazyListState = lazyListStateProvider[MeetingsTabItem.entries[it]]
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize()
            ) {
                for (i in 1..100) { item { Text(text = "Item $i") } }
            }
        }
    }
}

enum class MeetingsTabItem(@StringRes val titleResId: Int) : TabItem {
    UPCOMING(R.string.all_meetings_tab_upcoming),
    PAST(R.string.all_meetings_tab_past);

    override val title: UIText = UIText.StringResource(titleResId)
}

@PreviewMultipleThemes
@Composable
fun PreviewAllMeetingsScreen() = WireTheme {
    AllMeetingsScreen()
}

