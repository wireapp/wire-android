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
package com.wire.android.ui.home

import com.wire.android.datastore.UserDataStore
import com.wire.kalium.logic.feature.client.NeedsToRegisterClientUseCase
import com.wire.kalium.logic.feature.legalhold.ObserveLegalHoldStateForSelfUserUseCase
import com.wire.kalium.logic.feature.personaltoteamaccount.CanMigrateFromPersonalToTeamUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import dagger.Lazy
import dev.zacsweers.metro.Inject

@Inject
class HomeViewModelFactory(
    private val dataStore: UserDataStore,
    private val observeSelf: ObserveSelfUserUseCase,
    private val needsToRegisterClient: NeedsToRegisterClientUseCase,
    private val canMigrateFromPersonalToTeam: CanMigrateFromPersonalToTeamUseCase,
    private val observeLegalHoldStatusForSelfUser: ObserveLegalHoldStateForSelfUserUseCase,
    private val currentSessionFlow: Lazy<CurrentSessionFlowUseCase>,
) {
    fun create(): HomeViewModel = HomeViewModel(
        dataStore = dataStore,
        observeSelf = observeSelf,
        needsToRegisterClient = needsToRegisterClient,
        canMigrateFromPersonalToTeam = canMigrateFromPersonalToTeam,
        observeLegalHoldStatusForSelfUser = observeLegalHoldStatusForSelfUser,
        currentSessionFlow = currentSessionFlow,
    )
}
