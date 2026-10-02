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
import com.wire.kalium.cells.domain.CellUploadCoordinator
import com.wire.kalium.cells.domain.CellUploadItem
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.StateFlow

/**
 * Thin Compose-facing wrapper around [CellUploadCoordinator]: exposes its state and forwards every
 * user action straight back to it. Scheduling, concurrency and retry logic all stay in the coordinator.
 */
class UploadStatusViewModel @Inject constructor(
    private val coordinator: CellUploadCoordinator,
) : ViewModel() {

    val uploads: StateFlow<List<CellUploadItem>> = coordinator.uploads

    fun cancel(id: String) {
        coordinator.cancel(id)
    }

    fun cancelAll() {
        coordinator.cancelAll()
    }

    fun retry(id: String) {
        coordinator.retry(id)
    }

    fun retryAllFailed() {
        coordinator.retryAllFailed()
    }

    fun dismiss(id: String) {
        coordinator.dismiss(id)
    }
}
