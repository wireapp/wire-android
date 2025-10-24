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
                if (prevPair.second == newScrollIndex || newScrollIndex == prevPair.second + 1) {
                    prevPair
                } else {
                    prevPair.second to newScrollIndex
                }
            }
            .map { (prevScrollIndex, newScrollIndex) ->
                newScrollIndex > prevScrollIndex + 1
            }
            .distinctUntilChanged().collect { isScrollingDown ->
                onIsScrollingDown(isScrollingDown)
            }
    }
}
