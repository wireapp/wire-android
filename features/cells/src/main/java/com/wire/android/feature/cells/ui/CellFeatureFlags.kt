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

import com.wire.kalium.logic.featureFlags.KaliumConfigs
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named

/** The feature flags the files screens depend on, in one place. */
@Suppress("LongParameterList")
class CellFeatureFlags @Inject constructor(
    private val kaliumConfigs: KaliumConfigs,
    /** When disabled, all offline-files UI (save actions, offline banner, offline browsing) is hidden. */
    @Named("offlineFilesEnabled") val offlineFilesEnabled: Boolean,
    @Named("inAppImageViewerEnabled") val inAppImageViewerEnabled: Boolean,
    @Named("drivePermissionsEnabled") val drivePermissionsEnabled: Boolean,
    /** When enabled, drive content of a team without Drive becomes read-only instead of being hidden. */
    @Named("driveOffboardingEnabled") val driveOffboardingEnabled: Boolean,
    @Named("driveDirectUploadEnabled") val driveDirectUploadEnabled: Boolean,
    @Named("driveBulkActionsEnabled") val driveBulkActionsEnabled: Boolean,
    @Named("driveOfflineEnabled") val driveOfflineEnabled: Boolean,
    @Named("driveBulkRestoreEnabled") val bulkRestoreEnabled: Boolean,
) {
    val collaboraIntegration: Boolean get() = kaliumConfigs.collaboraIntegration

    /** Whether the bulk actions bar offers the offline actions (make available / remove offline access). */
    val bulkOfflineActionsEnabled: Boolean get() = offlineFilesEnabled && driveOfflineEnabled
}
