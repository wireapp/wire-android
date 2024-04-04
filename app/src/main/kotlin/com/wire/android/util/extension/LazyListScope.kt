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

package com.wire.android.util.extension

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wire.android.ui.home.conversationslist.common.FolderHeader

@OptIn(ExperimentalFoundationApi::class)
inline fun <T, K : Any> LazyListScope.folderWithElements(
    header: String? = null,
    items: Map<K, T>,
    animateItemPlacement: Boolean = true,
    crossinline divider: @Composable () -> Unit = {},
    crossinline factory: @Composable (T) -> Unit
) {
    val list = items.entries.toList()

    if (items.isNotEmpty()) {
        if (!header.isNullOrEmpty()) {
            item(key ="header:${header}") {
                FolderHeader(
                    name = header,
                    modifier = Modifier
                        .fillMaxWidth()
                        .let { if (animateItemPlacement) it.animateItemPlacement() else it }
                )
            }
        }
        itemsIndexed(
            items = list,
            key = { _: Int, item: Map.Entry<K, T> -> "$item:${item.key}" }
        ) { index: Int, item: Map.Entry<K, T> ->
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .let { if (animateItemPlacement) it.animateItemPlacement() else it }
            ) {
                factory(item.value)
                if (index <= list.lastIndex) {
                    divider()
                }
            }
        }
    }
}
