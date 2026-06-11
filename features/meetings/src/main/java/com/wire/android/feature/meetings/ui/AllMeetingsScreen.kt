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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import com.wire.android.feature.meetings.R
import com.wire.android.feature.meetings.ui.list.MeetingList
import com.wire.android.feature.meetings.ui.options.MeetingOptionsModalSheetLayout
import com.wire.android.feature.meetings.ui.util.PreviewMultipleThemes
import com.wire.android.ui.common.TabItem
import com.wire.android.ui.common.WireTabRow
import com.wire.android.ui.common.bottomsheet.WireBottomSheetDefaults
import com.wire.android.ui.common.bottomsheet.WireSheetValue
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.calculateCurrentTab
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.rememberLazyListStateProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

@SuppressLint("ComposeModifierMissing")
@Composable
fun AllMeetingsScreen(
    lazyListState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    tabs: ImmutableList<MeetingsTabItem> = persistentListOf(MeetingsTabItem.NEXT), // history is not available yet, only "Next" for now
    initialTab: MeetingsTabItem = MeetingsTabItem.NEXT,
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        val scope = rememberCoroutineScope()
        val pagerState = rememberPagerState(tabs.indexOf(initialTab).coerceIn(0, tabs.size - 1)) { tabs.size }
        val lazyListStateProvider = rememberLazyListStateProvider<MeetingsTabItem>()
        val meetingOptionsSheetState = rememberWireModalSheetState<String>(initialValue = WireSheetValue.Hidden)

        if (tabs.size > 1) {
            Surface(
                color = WireBottomSheetDefaults.WireSheetContainerColor,
                shadowElevation = lazyListStateProvider[tabs[pagerState.currentPage]].rememberTopBarElevationState().value,
                contentColor = colorsScheme().background,
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(1f) // Ensure tab row is above the lazy column (for elevation shadow)
            ) {
                WireTabRow(
                    tabs = tabs,
                    selectedTabIndex = pagerState.calculateCurrentTab(),
                    onTabChange = {
                        scope.launch {
                            pagerState.animateScrollToPage(it)
                        }
                    }
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) {
            MeetingList(
                modifier = Modifier.fillMaxSize(),
                lazyListState = if (tabs.size > 1) lazyListStateProvider[tabs[it]] else lazyListState,
                type = tabs[it],
                contentPadding = contentPadding,
                openMeetingOptions = { meetingId ->
                    meetingOptionsSheetState.show(meetingId)
                }
            )
        }

        MeetingOptionsModalSheetLayout(sheetState = meetingOptionsSheetState)
    }
}

enum class MeetingsTabItem(@StringRes val titleResId: Int) : TabItem {
    NEXT(R.string.all_meetings_tab_next),
    PAST(R.string.all_meetings_tab_past);

    override val title: UIText = UIText.StringResource(titleResId)
}

@PreviewMultipleThemes
@Composable
fun PreviewAllMeetingsScreen_OnlyNextTab() = WireTheme {
    AllMeetingsScreen(tabs = persistentListOf(MeetingsTabItem.NEXT))
}

@PreviewMultipleThemes
@Composable
fun PreviewAllMeetingsScreen_NextAndPastTabs() = WireTheme {
    AllMeetingsScreen(tabs = persistentListOf(MeetingsTabItem.NEXT, MeetingsTabItem.PAST), initialTab = MeetingsTabItem.PAST)
}
