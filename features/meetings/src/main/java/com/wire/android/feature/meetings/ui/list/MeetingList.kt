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

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.wire.android.feature.meetings.R
import com.wire.android.feature.meetings.model.MeetingHeader
import com.wire.android.feature.meetings.model.MeetingItem
import com.wire.android.feature.meetings.model.MeetingListItem
import com.wire.android.feature.meetings.ui.MeetingsTabItem
import com.wire.android.feature.meetings.ui.util.CurrentTimeProvider
import com.wire.android.feature.meetings.ui.util.PreviewMultipleThemes
import com.wire.android.ui.common.rowitem.EmptyListArrowFooter
import com.wire.android.ui.common.rowitem.EmptyListContent
import com.wire.android.ui.common.rowitem.LoadingListContent
import com.wire.android.ui.theme.WireTheme

@Composable
fun MeetingList(
    type: MeetingsTabItem,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    meetingListViewModel: MeetingListViewModel = when {
        LocalInspectionMode.current -> MeetingListViewModelPreview(CurrentTimeProvider.Preview, type)
        else -> hiltViewModel<MeetingListViewModelImpl, MeetingListViewModelImpl.Factory>(
            key = "meeting_list_${type.name}",
            creationCallback = { factory ->
                factory.create(type = type)
            }
        )
    },
) {
    val lazyPagingItems = meetingListViewModel.meetings.collectAsLazyPagingItems()
    val isShowingAll = meetingListViewModel.isShowingAll.collectAsState().value
    val showLoading = lazyPagingItems.loadState.refresh == LoadState.Loading && lazyPagingItems.itemCount == 0
    AnimatedContent(targetState = showLoading to (lazyPagingItems.itemCount == 0)) { (loading, emptyList) ->
        when {
            loading -> LoadingListContent(
                lazyListState = lazyListState,
                modifier = modifier
            )

            emptyList -> EmptyMeetingListContent(
                type = type,
                modifier = modifier
            )

            else -> MeetingList(
                lazyPagingItems = lazyPagingItems,
                lazyListState = lazyListState,
                isShowingAll = isShowingAll,
                onShowAll = meetingListViewModel::showAll,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun MeetingList(
    lazyListState: LazyListState,
    lazyPagingItems: LazyPagingItems<MeetingListItem>,
    isShowingAll: Boolean,
    onShowAll: () -> Unit,
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
                    is MeetingHeader.Ongoing -> "separator_ongoing"
                    is MeetingHeader.DayAndHour -> "separator_day_and_hour_${it.time.toEpochMilliseconds()}"
                    is MeetingHeader.Hour -> "separator_hour_${it.time.toEpochMilliseconds()}"
                    is MeetingItem -> it.meetingId
                }
            },
            contentType = lazyPagingItems.itemContentType { it::class.simpleName },
        ) { index ->
            lazyPagingItems[index]?.let { item ->
                when (item) {
                    is MeetingHeader -> MeetingHeader(header = item, modifier = Modifier.animateItem())
                    is MeetingItem -> MeetingItem(meeting = item, modifier = Modifier.animateItem())
                }
            }
        }
        val endOfPaginationReached = (lazyPagingItems.loadState.append as? LoadState.NotLoading)?.endOfPaginationReached ?: false
        val isLoadingMore = lazyPagingItems.loadState.append == LoadState.Loading
        when {
            isLoadingMore && !endOfPaginationReached -> item(key = "footer_load_more", contentType = "footer_load_more") {
                MeetingLoadMoreFooter()
            }

            endOfPaginationReached && !isShowingAll -> item(key = "footer_show_all", contentType = "footer_show_all") {
                MeetingShowAllFooter(onShowAll = onShowAll)
            }
        }
    }
}

@Composable
private fun EmptyMeetingListContent(type: MeetingsTabItem, modifier: Modifier = Modifier) {
    EmptyListContent(
        title = when (type) {
            MeetingsTabItem.NEXT -> stringResource(R.string.meetings_empty_title_next)
            MeetingsTabItem.PAST -> stringResource(R.string.meetings_empty_title_past)
        },
        text = when (type) {
            MeetingsTabItem.NEXT -> stringResource(R.string.meetings_empty_text_next)
            MeetingsTabItem.PAST -> stringResource(R.string.meetings_empty_text_past)
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
    MeetingList(type = MeetingsTabItem.NEXT)
}

@PreviewMultipleThemes
@Composable
fun MeetingListPastPreview() = WireTheme {
    MeetingList(type = MeetingsTabItem.PAST)
}
