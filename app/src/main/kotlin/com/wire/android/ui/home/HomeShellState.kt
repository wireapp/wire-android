/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.home

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.DrawerState
import androidx.compose.ui.focus.FocusRequester
import com.wire.android.navigation.HomeDestination
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.search.SearchBarState
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.ConversationFilter
import kotlinx.coroutines.CoroutineScope

/**
 * Navigation-neutral state owned by the Navigation 3 Home entry.
 *
 * Top-level Home destinations are content selections rather than nested back-stack entries, so
 * drawer, search, filters and per-content scroll state survive selection changes in one owner.
 */
interface HomeShellState {
    val coroutineScope: CoroutineScope
    val drawerState: DrawerState
    val searchBarState: SearchBarState
    val conversationsFilterBottomSheetState: WireModalSheetState<Unit>
    val newMeetingBottomSheetState: WireModalSheetState<Unit>
    val emptySearchResultFocusRequester: FocusRequester
    val firstConversationFocusRequester: FocusRequester
    val currentNavigationItem: HomeDestination
    val currentConversationFilter: ConversationFilter
    val currentTitle: UIText

    fun lazyListStateFor(
        destination: HomeDestination,
        conversationFilter: ConversationFilter = ConversationFilter.All,
    ): LazyListState

    fun closeDrawer()
    fun openDrawer()
    fun changeConversationFilter(filter: ConversationFilter)
    fun requestClearSearchOnNextResume()
    fun clearSearchOnResumeIfRequested()
}
