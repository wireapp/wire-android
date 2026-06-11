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

package com.wire.android.ui.common.search

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.saveable.SaverScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SearchBarStateTest {

    @Test
    fun `given search visibility changed, when state is restored, then visibility is preserved`() {
        val state = SearchBarState(
            isSearchActive = true,
            searchQueryTextState = TextFieldState(initialText = "query")
        )
        state.searchVisibleChanged(false)

        val restored = with(SearchBarState.saver()) {
            val saved = SaverScope { true }.save(state)
            restore(saved!!)
        }!!

        assertTrue(restored.isSearchActive)
        assertFalse(restored.isSearchVisible)
        assertEquals("query", restored.searchQueryTextState.text.toString())
    }

    @Test
    fun `given old saved search state, when state is restored, then query is preserved`() {
        val savedQuery = with(TextFieldState.Saver) {
            SaverScope { true }.save(TextFieldState(initialText = "query"))
        }

        val restored = SearchBarState.saver().restore(listOf(true, savedQuery))!!

        assertTrue(restored.isSearchActive)
        assertTrue(restored.isSearchVisible)
        assertEquals("query", restored.searchQueryTextState.text.toString())
    }
}
