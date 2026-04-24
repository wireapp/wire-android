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
 * Collects a [Flow] of [PagingData] as [LazyPagingItems] that are lifecycle-aware.
 *
 * This function ensures that the collection of the paging data is tied to the lifecycle of the
 * composable, preventing memory leaks and unnecessary work when the composable is not active.
 *
 * @param minActiveState The minimum lifecycle state at which the flow should be collected. Default is [Lifecycle.State.RESUMED].
 * @param context The coroutine context to use for collecting the flow. Default is [EmptyCoroutineContext].
 * @param lifecycle The lifecycle to use for determining when to collect the flow. Default is the current one from [LocalLifecycleOwner].
 * @return A [LazyPagingItems] instance that can be used in a LazyColumn or similar composable.
 */
@Composable
fun <T : Any> Flow<PagingData<T>>.collectAsLazyPagingItemsWithLifecycle(
    minActiveState: Lifecycle.State = Lifecycle.State.RESUMED,
    context: CoroutineContext = EmptyCoroutineContext,
    lifecycle: Lifecycle = LocalLifecycleOwner.current.lifecycle
): LazyPagingItems<T> {
    val isPreview = LocalInspectionMode.current
    val lifecycleAwareFlow = remember(this, lifecycle, isPreview) {
        if (isPreview) this else this.flowWithLifecycle(lifecycle, minActiveState)
    }
    return lifecycleAwareFlow.collectAsLazyPagingItems(context)
}
