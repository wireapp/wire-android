/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.di.accountScoped

import com.wire.android.di.CurrentAccount
import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.team.SyncSelfTeamInfoUseCase
import com.wire.kalium.logic.feature.team.TeamScope
import com.wire.kalium.logic.feature.user.IsSelfATeamMemberUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
class TeamModule {

    @ViewModelScoped
    @Provides
    fun provideTeamScope(
        @CurrentAccount currentAccount: UserId,
        @KaliumCoreLogic coreLogic: CoreLogic
    ): TeamScope = coreLogic.getSessionScope(currentAccount).team

    @ViewModelScoped
    @Provides
    fun provideSyncSelfTeamInfoUseCase(teamScope: TeamScope): SyncSelfTeamInfoUseCase =
        teamScope.syncSelfTeamInfoUseCase

    @ViewModelScoped
    @Provides
    fun provideIsSelfATeamMemberUseCase(teamScope: TeamScope): IsSelfATeamMemberUseCase =
        teamScope.isSelfATeamMember
}
