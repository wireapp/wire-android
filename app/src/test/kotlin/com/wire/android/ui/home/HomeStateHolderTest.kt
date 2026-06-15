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

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.NavHostController
import com.wire.android.navigation.HomeDestination
import com.wire.android.navigation.Navigator
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.search.SearchBarState
import com.wire.android.ui.common.topappbar.ConversationFilterState
import com.wire.android.util.ui.LazyListStateProvider
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.coroutines.EmptyCoroutineContext

class HomeStateHolderTest {

    @Test
    fun `given active search with query when clear on next resume is requested then search is kept until resume`() {
        val state = arrangement()

        state.requestClearSearchOnNextResume()

        assertTrue(state.searchBarState.isSearchActive)
        assertEquals("query", state.searchBarState.searchQueryTextState.text.toString())

        state.clearSearchOnResumeIfRequested()

        assertFalse(state.searchBarState.isSearchActive)
        assertEquals("", state.searchBarState.searchQueryTextState.text.toString())
    }

    @Test
    fun `given clear on next resume was consumed when resume happens again then search is not cleared again`() {
        val state = arrangement()

        state.requestClearSearchOnNextResume()
        state.clearSearchOnResumeIfRequested()
        state.searchBarState.openSearch()
        state.searchBarState.searchQueryTextState.setTextAndPlaceCursorAtEnd("new query")

        state.clearSearchOnResumeIfRequested()

        assertTrue(state.searchBarState.isSearchActive)
        assertEquals("new query", state.searchBarState.searchQueryTextState.text.toString())
    }

    @Test
    fun `given clear on next resume was not requested when resume happens then search is preserved`() {
        val state = arrangement()

        state.clearSearchOnResumeIfRequested()

        assertTrue(state.searchBarState.isSearchActive)
        assertEquals("query", state.searchBarState.searchQueryTextState.text.toString())
    }

    private fun arrangement(
        searchBarState: SearchBarState = SearchBarState(
            isSearchActive = true,
            searchQueryTextState = TextFieldState(initialText = "query")
        )
    ) = HomeStateHolder(
        coroutineScope = CoroutineScope(EmptyCoroutineContext),
        navController = mockk<NavHostController>(relaxed = true),
        drawerState = mockk<DrawerState>(relaxed = true),
        searchBarState = searchBarState,
        conversationsFilterBottomSheetState = mockk<WireModalSheetState<Unit>>(relaxed = true),
        newMeetingBottomSheetState = mockk<WireModalSheetState<Unit>>(relaxed = true),
        navigator = Navigator(finish = {}, navController = mockk(relaxed = true)),
        currentNavigationItemState = mutableStateOf(HomeDestination.Conversations),
        conversationFilterState = ConversationFilterState(),
        lazyListStateProvider = LazyListStateProvider(),
    )
}
