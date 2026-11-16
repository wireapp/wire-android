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
package com.wire.android.feature.cells.ui

import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.model.isOfflineDownloadPending
import dev.zacsweers.metro.Inject

/** Which files can be saved for offline use, or have their offline copy removed. */
class OfflineAccessRules @Inject constructor(
    private val featureFlags: CellFeatureFlags,
) {

    /** Same rule as the single-item menu, see [CellFileActionsMenu]. */
    internal fun canBeMadeAvailableOffline(file: CellNodeUi.File): Boolean =
        !file.isAvailableOffline && !file.isOfflineDownloadPending() && featureFlags.collaboraIntegration && !file.isEditSupported

    /** At least one of [nodes] can be saved for offline use with the bulk actions, see [canBeMadeAvailableOffline]. */
    internal fun canMakeAnyAvailableOffline(nodes: Collection<CellNodeUi>): Boolean =
        featureFlags.bulkOfflineActionsEnabled && nodes.any { it is CellNodeUi.File && canBeMadeAvailableOffline(it) }

    /** At least one of [nodes] is already saved for offline use, and can have its copy removed with the bulk actions. */
    internal fun canRemoveAnyOfflineAccess(nodes: Collection<CellNodeUi>): Boolean =
        featureFlags.bulkOfflineActionsEnabled && nodes.any { it is CellNodeUi.File && it.isAvailableOffline }
}
