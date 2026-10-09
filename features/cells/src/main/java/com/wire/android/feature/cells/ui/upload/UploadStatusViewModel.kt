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
package com.wire.android.feature.cells.ui.upload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.kalium.cells.domain.CellUploadCoordinator
import com.wire.kalium.cells.domain.CellUploadItem
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Thin Compose-facing wrapper around [CellUploadCoordinator], scoped to a single conversation's Shared
 * Drive: [uploads] only ever exposes items whose [CellUploadItem.conversationId] is [conversationId], and
 * every "all" action is scoped the same way, so an upload running in one conversation never shows up in, or
 * is affected by, another conversation's bottom sheet. Scheduling, concurrency and retry logic all stay in
 * the coordinator, which is shared by every conversation.
 */
class UploadStatusViewModel @AssistedInject constructor(
    @Assisted private val conversationId: String?,
    private val coordinator: CellUploadCoordinator,
) : ViewModel() {

    val uploads: StateFlow<List<CellUploadItem>> = coordinator.uploads
        .map { items -> items.filter { it.conversationId == conversationId } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = coordinator.uploads.value.filter { it.conversationId == conversationId },
        )

    fun cancel(id: String) {
        coordinator.cancel(id)
    }

    fun cancelAll() {
        conversationId?.let(coordinator::cancelAll)
    }

    fun retry(id: String) {
        coordinator.retry(id)
    }

    fun retryAllFailed() {
        conversationId?.let(coordinator::retryAllFailed)
    }

    fun dismiss(id: String) {
        coordinator.dismiss(id)
    }

    fun dismissAll() {
        conversationId?.let(coordinator::dismissAll)
    }

    @AssistedFactory
    interface Factory {
        fun create(conversationId: String?): UploadStatusViewModel
    }
}