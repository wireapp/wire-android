package com.wire.android.ui.home

import io.github.esentsov.PackagePrivate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan

class HomeScrollBridge @PackagePrivate constructor() {

    private var scrollIndexFlow = MutableStateFlow(0)

    val scrollDownFlow: Flow<Boolean> = scrollIndexFlow
        .scan(0 to 0) { prevPair, newScrollIndex ->
            if (prevPair.second == newScrollIndex || newScrollIndex == prevPair.second + 1) prevPair
            else prevPair.second to newScrollIndex
        }
        .map { (prevScrollIndex, newScrollIndex) ->
            newScrollIndex > prevScrollIndex + 1
        }
        .distinctUntilChanged()

    suspend fun updateScrollPosition(newScrollIndex: Int) {
        scrollIndexFlow.emit(newScrollIndex)
    }
}
