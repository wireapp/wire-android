package com.wire.android.ui.main.conversation.common.extension

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import com.wire.android.ui.main.conversation.common.FolderHeader


inline fun <T> LazyListScope.folderWithElements(
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
