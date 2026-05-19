/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Collects a [Flow] of [PagingData] as [LazyPagingItems] tied to the host lifecycle, so
 * paging work pauses while the screen is not visible (e.g. when the app is in the
 * background or the destination is off‑screen).
 *
 * `flowWithLifecycle` cancels and restarts the consumer on lifecycle dips. This works
 * cleanly only when the upstream is a hot, shared source (typically produced via
 * `cachedIn(viewModelScope).shareIn(viewModelScope, replay = 1)` on the ViewModel side).
 * Wrapping a non‑shared `cachedIn` flow here would force Paging to treat each resume as
 * a fresh subscriber, dropping the cached pages and the scroll position. The empty
 * `LazyPagingItems` paging‑compose 3.3.x emits on each restart is masked at the UI
 * layer by the ViewModel‑side item snapshot cache.
 *
 * @param minActiveState minimum lifecycle state at which the flow is collected.
 * @param context coroutine context for collection.
 * @param lifecycle host lifecycle; defaults to [LocalLifecycleOwner].
 */
@Composable
fun <T : Any> Flow<PagingData<T>>.collectAsLazyPagingItemsWithLifecycle(
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    context: CoroutineContext = EmptyCoroutineContext,
    lifecycle: Lifecycle = LocalLifecycleOwner.current.lifecycle,
): LazyPagingItems<T> {
    val isPreview = LocalInspectionMode.current
    val lifecycleAwareFlow = remember(this, lifecycle, isPreview) {
        if (isPreview) this else this.flowWithLifecycle(lifecycle, minActiveState)
    }
    return lifecycleAwareFlow.collectAsLazyPagingItems(context)
}
