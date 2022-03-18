package com.wire.android.ui.common.effects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan


/**
 * An effect taking [scrollPosition] and [onIsScrollingDown] lambda.
 * [scrollPosition] is a "feed" value for the effect, that will call the [onIsScrollingDown]
 * whenever the direction of the scrolling changes using the scrollPosition provided from the parent
 * when that happens the [onIsScrollingDown] will emit a Boolean value :
 * true on scrolling down
 * false on scrolling up
 */
@Composable
inline fun ScrollingDownEffect(scrollPosition: Int, crossinline onIsScrollingDown: (Boolean) -> Unit) {
    LaunchedEffect(scrollPosition) {
        snapshotFlow { scrollPosition }
            .scan(0 to 0) { prevPair, newScrollIndex ->
                if (prevPair.second == newScrollIndex || newScrollIndex == prevPair.second + 1) prevPair
                else prevPair.second to newScrollIndex
            }
            .map { (prevScrollIndex, newScrollIndex) ->
                newScrollIndex > prevScrollIndex + 1
            }
            .distinctUntilChanged().collect { isScrollingDown ->
                onIsScrollingDown(isScrollingDown)
            }
    }
}
