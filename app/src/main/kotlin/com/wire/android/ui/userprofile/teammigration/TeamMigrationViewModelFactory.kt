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
package com.wire.android.ui.userprofile.teammigration

import com.wire.android.datastore.UserDataStore
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.kalium.logic.feature.server.GetTeamUrlUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import com.wire.kalium.logic.feature.user.migration.MigrateFromPersonalToTeamUseCase
import dev.zacsweers.metro.Inject

@Inject
class TeamMigrationViewModelFactory(
    private val anonymousAnalyticsManager: AnonymousAnalyticsManager,
    private val migrateFromPersonalToTeam: MigrateFromPersonalToTeamUseCase,
    private val observeSelfUser: ObserveSelfUserUseCase,
    private val dataStore: UserDataStore,
    private val getTeamUrl: GetTeamUrlUseCase,
) {
    fun create(): TeamMigrationViewModel = TeamMigrationViewModel(
        anonymousAnalyticsManager = anonymousAnalyticsManager,
        migrateFromPersonalToTeam = migrateFromPersonalToTeam,
        observeSelfUser = observeSelfUser,
        dataStore = dataStore,
        getTeamUrl = getTeamUrl,
    )
}
