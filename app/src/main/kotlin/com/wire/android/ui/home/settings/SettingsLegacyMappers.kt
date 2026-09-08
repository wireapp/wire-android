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

package com.wire.android.ui.home.settings

import com.wire.navigation.SessionRoute
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId

/**
 * Temporary adapter at the settings-list boundary.
 *
 * The settings screen can adopt this mapping before the legacy [SettingsItem.DirectionItem]
 * representation is removed. Items outside this migration batch deliberately return `null`.
 */
internal fun SettingsItem.toNavigation3Route(
    sessionId: WireSessionId,
    entryId: WireNavEntryId = WireNavEntryId.random(),
): SessionRoute? = when (this) {
    SettingsItem.AppSettings -> AppSettingsRoute(sessionId, entryId)
    SettingsItem.YourAccount -> MyAccountRoute(sessionId, entryId)
    SettingsItem.AboutApp -> AboutThisAppRoute(sessionId, entryId)
    SettingsItem.NetworkSettings -> NetworkSettingsRoute(sessionId, entryId)
    SettingsItem.PrivacySettings -> PrivacySettingsRoute(sessionId, entryId)
    SettingsItem.Customization -> CustomizationSettingsRoute(sessionId, entryId)
    SettingsItem.Licenses -> LicensesSettingsRoute(sessionId, entryId)
    SettingsItem.Dependencies -> DependenciesSettingsRoute(sessionId, entryId)
    SettingsItem.BackupAndRestore -> BackupAndRestoreSettingsRoute(sessionId, entryId)
    else -> null
}
