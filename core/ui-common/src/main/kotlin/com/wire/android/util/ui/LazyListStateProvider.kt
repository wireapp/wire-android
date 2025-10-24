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

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable

class LazyListStateProvider<T : Any> {
    private val map = mutableMapOf<T, LazyListState>()
    operator fun get(key: T): LazyListState = map.getOrPut(key) { LazyListState() }

    companion object {
        fun <T : Any> saver(): Saver<LazyListStateProvider<T>, Any> = listSaver(
            save = {
                it.map.entries.map { (key, lazyListState) ->
                    key to listOf(lazyListState.firstVisibleItemIndex, lazyListState.firstVisibleItemScrollOffset)
                }
            },
            restore = {
                LazyListStateProvider<T>().apply {
                    map.putAll(
                        it.associate { (key, list) ->
                            key to LazyListState(firstVisibleItemIndex = list[0], firstVisibleItemScrollOffset = list[1])
                        }
                    )
                }
            }
        )
    }
}

@Composable
fun <T : Any> rememberLazyListStateProvider() = rememberSaveable(saver = LazyListStateProvider.saver<T>()) {
    LazyListStateProvider()
}
