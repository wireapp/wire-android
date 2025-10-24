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
package com.wire.android.ui.common.rowitem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.PreviewMultipleThemes

@Composable
fun LoadingListContent(
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxSize()
    ) {
        items(count = LOADING_PLACEHOLDER_ITEMS_COUNT) { index ->
            val halfItemsCount = LOADING_PLACEHOLDER_ITEMS_COUNT / 2
            val alpha = if (index < halfItemsCount) 1f else 1f - ((index - halfItemsCount + 1).toFloat() / (halfItemsCount + 1))
            Box(modifier = Modifier.alpha(alpha)) {
                LoadingRowItem()
            }
        }
    }
}

private const val LOADING_PLACEHOLDER_ITEMS_COUNT = 6

@PreviewMultipleThemes
@Composable
fun PreviewLoadingListContent() = WireTheme {
    LoadingListContent(lazyListState = rememberLazyListState())
}
