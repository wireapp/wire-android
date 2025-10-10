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
package com.wire.android.feature.meetings.ui.list

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.wire.android.feature.meetings.model.MeetingItem
import com.wire.android.feature.meetings.model.MeetingListItem
import com.wire.android.feature.meetings.model.MeetingSeparator
import com.wire.android.feature.meetings.ui.MeetingsTabItem
import com.wire.android.feature.meetings.ui.util.CurrentTimeScope
import com.wire.android.feature.meetings.ui.util.PreviewMultipleThemes
import com.wire.android.feature.meetings.ui.util.previewCurrentTimeScope
import com.wire.android.ui.common.rowitem.EmptyListArrowFooter
import com.wire.android.ui.common.rowitem.EmptyListContent
import com.wire.android.ui.common.rowitem.LoadingListContent
import com.wire.android.ui.theme.WireTheme

@Composable
fun CurrentTimeScope.MeetingList(
    type: MeetingsTabItem,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    meetingListViewModel: MeetingListViewModel = when {
        LocalInspectionMode.current -> MeetingListViewModelPreview(this, type)
        else -> hiltViewModel<MeetingListViewModelImpl, MeetingListViewModelImpl.Factory>(
            key = "meeting_list_${type.name}",
            creationCallback = { factory ->
                factory.create(type = type)
            }
        )
    },
) {
    val lazyPagingItems = meetingListViewModel.meetings.collectAsLazyPagingItems()
    val showLoading = lazyPagingItems.loadState.refresh == LoadState.Loading && lazyPagingItems.itemCount == 0
    when {
        showLoading -> LoadingListContent(
            lazyListState = lazyListState,
            modifier = modifier
        )

        lazyPagingItems.itemCount == 0 -> EmptyMeetingListContent(
            type = type,
            modifier = modifier
        )

        else -> MeetingList(
            lazyPagingItems = lazyPagingItems,
            lazyListState = lazyListState,
            showAll = meetingListViewModel::showAll,
        )
    }
}

@Composable
private fun CurrentTimeScope.MeetingList(
    lazyPagingItems: LazyPagingItems<MeetingListItem>,
    lazyListState: LazyListState,
    showAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = lazyListState,
        modifier = modifier,
    ) {
        items(
            count = lazyPagingItems.itemCount,
            key = lazyPagingItems.itemKey {
                when (it) {
                    is MeetingSeparator.Ongoing -> "separator_ongoing"
                    is MeetingSeparator.DayAndHour -> "separator_day_and_hour_${it.time.toEpochMilliseconds()}"
                    is MeetingSeparator.Hour -> "separator_hour_${it.time.toEpochMilliseconds()}"
                    is MeetingSeparator.ShowAll -> "footer_show_all"
                    is MeetingItem -> it.meetingId
                }
            },
            contentType = lazyPagingItems.itemContentType { it::class.simpleName },
            itemContent = { index ->
                lazyPagingItems[index]?.let { item ->
                    when (item) {
                        is MeetingSeparator -> MeetingSeparator(
                            header = item,
                            onShowAll = showAll,
                            modifier = Modifier.animateItem(),
                        )
                        is MeetingItem -> MeetingItem(
                            meeting = item,
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun EmptyMeetingListContent(type: MeetingsTabItem, modifier: Modifier = Modifier) {
    EmptyListContent(
        title = when (type) {
            MeetingsTabItem.NEXT -> "No upcoming meetings yet"
            MeetingsTabItem.PAST -> "No past meetings"
        },
        text = when (type) {
            MeetingsTabItem.NEXT -> "Start a meeting with team members, guests, or external parties. "
            MeetingsTabItem.PAST -> "Previous meetings will be liste here"
        },
        footer = {
            if (type == MeetingsTabItem.NEXT) EmptyListArrowFooter()
        },
        modifier = modifier,
    )
}

@PreviewMultipleThemes
@Composable
fun MeetingListNextEmptyPreview() = WireTheme {
    EmptyMeetingListContent(type = MeetingsTabItem.NEXT)
}

@PreviewMultipleThemes
@Composable
fun MeetingListPastEmptyPreview() = WireTheme {
    EmptyMeetingListContent(type = MeetingsTabItem.PAST)
}

@PreviewMultipleThemes
@Composable
fun MeetingListNextPreview() = WireTheme {
    previewCurrentTimeScope.MeetingList(type = MeetingsTabItem.NEXT)
}

@PreviewMultipleThemes
@Composable
fun MeetingListPastPreview() = WireTheme {
    previewCurrentTimeScope.MeetingList(type = MeetingsTabItem.PAST)
}
