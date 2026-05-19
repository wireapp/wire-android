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
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Collects a [Flow] of [PagingData] as [LazyPagingItems].
 *
 * Important: the upstream flow is expected to be a hot, shared source (e.g. produced by
 * `cachedIn(viewModelScope).shareIn(...)`). Wrapping the upstream with `flowWithLifecycle`
 * here would cancel and restart the collector on every lifecycle dip, which defeats
 * `cachedIn` — Paging would treat each return-to-screen as a fresh subscriber and drop
 * the cached pages along with the scroll position. Subscription lifecycle is handled
 * internally by `collectAsLazyPagingItems` via `DisposableEffect`.
 */
@Composable
fun <T : Any> Flow<PagingData<T>>.collectAsLazyPagingItemsWithLifecycle(
    context: CoroutineContext = EmptyCoroutineContext,
): LazyPagingItems<T> = collectAsLazyPagingItems(context)
