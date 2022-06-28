package com.wire.android.ui.home.conversationslist

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import com.wire.android.ui.home.conversationslist.common.FolderHeader

inline fun <T, K : Any> LazyListScope.folderWithElements(
    header: String,
    items: Map<K, T>,
    crossinline divider: @Composable () -> Unit = {},
    crossinline factory: @Composable (T) -> Unit
) {
    val list = items.entries.toList()
    if (items.isNotEmpty()) {
        item(key = "header:$header") { FolderHeader(name = header) }
        itemsIndexed(
            items = list,
            key = { _: Int, item: Map.Entry<K, T> -> item.key })
        { index: Int, item: Map.Entry<K, T> ->
            factory(item.value)
            if (index < list.lastIndex)
                divider()
        }
    }
}
