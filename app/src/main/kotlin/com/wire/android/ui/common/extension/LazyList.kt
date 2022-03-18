package com.wire.android.ui.common.extension

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun rememberLazyListState(
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
    onScrollPositionChanged: (Int) -> Unit,
): LazyListState {
    val state = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState(
            initialFirstVisibleItemIndex,
            initialFirstVisibleItemScrollOffset
        )
    }

    onScrollPositionChanged(state.firstVisibleItemIndex)

    return state
}
