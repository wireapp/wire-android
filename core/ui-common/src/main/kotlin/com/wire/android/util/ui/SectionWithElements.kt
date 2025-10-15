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

package com.wire.android.util.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wire.android.ui.common.rowitem.CollapsingSectionHeader
import com.wire.android.ui.common.rowitem.SectionHeader

@Suppress("LongParameterList", "CyclomaticComplexMethod")
inline fun <T, K : Any> LazyListScope.sectionWithElements(
    header: String? = null,
    items: Map<K, T>,
    animateItemPlacement: Boolean = true,
    folderType: FolderType = FolderType.Regular,
    crossinline divider: @Composable () -> Unit = {},
    crossinline factory: @Composable (T) -> Unit
) {
    val list = items.entries.toList()

    if (items.isNotEmpty()) {
        if (!header.isNullOrEmpty()) {
            item(key = "header:$header") {
                when (folderType) {
                    is FolderType.Collapsing -> CollapsingSectionHeader(
                        expanded = folderType.expanded,
                        onClicked = folderType.onChanged,
                        name = header,
                        modifier = Modifier
                            .fillMaxWidth()
                            .let { if (animateItemPlacement) it.animateItem() else it }
                    )

                    is FolderType.Regular -> SectionHeader(
                        name = header,
                        modifier = Modifier
                            .fillMaxWidth()
                            .let { if (animateItemPlacement) it.animateItem() else it }
                    )
                }
            }
        }
        if (folderType is FolderType.Collapsing && !folderType.expanded) {
            return // do not show items if the folder is collapsed
        }
        itemsIndexed(
            items = list,
            key = { _: Int, item: Map.Entry<K, T> -> "$item:${item.key}" }
        ) { index: Int, item: Map.Entry<K, T> ->
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .let { if (animateItemPlacement) it.animateItem() else it }
            ) {
                factory(item.value)
                if (index <= list.lastIndex) {
                    divider()
                }
            }
        }
    }
}

sealed interface FolderType {
    data class Collapsing(val expanded: Boolean, val onChanged: (Boolean) -> Unit) : FolderType
    data object Regular : FolderType
}
