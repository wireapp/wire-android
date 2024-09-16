/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.util.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExplicitGroupsComposable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.SideEffect

/**
 * When the list is scrolled to top and new items (e.g. new activity section) should appear on top of the list, it appears above
 * all current items, scroll is preserved so the list still shows the same item as the first one on list so it scrolls
 * automatically to that item and the newly added section on top is hidden above this previously top item, so for such situation
 * when the list is scrolled to the top and we want the new section to appear at the top we request to scroll to item at the top.
 * Implemented according to the templates from compose lazy list test cases - LazyListRequestScrollTest.kt.
 * https://android.googlesource.com/platform/frameworks/support/+/refs/changes/93/2987293/35/compose/foundation/foundation/integration-tests/lazy-tests/src/androidTest/kotlin/androidx/compose/foundation/lazy/list/LazyListRequestScrollTest.kt
 */
@Composable
@NonRestartableComposable
@ExplicitGroupsComposable
fun KeepOnTopWhenNotScrolled(lazyListState: LazyListState) = SideEffect {
        if (lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0) {
            lazyListState.requestScrollToItem(
                index = lazyListState.firstVisibleItemIndex,
                scrollOffset = lazyListState.firstVisibleItemScrollOffset
            )
        }
    }
