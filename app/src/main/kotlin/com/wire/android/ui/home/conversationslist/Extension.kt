package com.wire.android.ui.home.conversationslist

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import com.wire.android.ui.home.conversationslist.common.FolderHeader

inline fun <T> LazyListScope.folderWithElementsAndRegularHeader(
    crossinline header: @Composable () -> String,
    items: List<T>,
    crossinline factory: @Composable (T) -> Unit
) {
    if (items.isNotEmpty()) {
        item { FolderHeader(name = header()) }
        items(items) {
            factory(it)
        }
    }
}

