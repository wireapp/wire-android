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
package com.wire.android.ui.home.settings.account

import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.feature.user.IsE2EIEnabledUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import com.wire.kalium.logic.feature.user.IsReadOnlyAccountUseCase
import com.wire.kalium.logic.feature.user.IsSelfATeamMemberUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserWithTeamUseCase
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import dev.zacsweers.metro.Inject

@Inject
@Suppress("LongParameterList")
class MyAccountViewModelFactory(
    private val getSelf: ObserveSelfUserUseCase,
    private val observeSelfUserWithTeam: ObserveSelfUserWithTeamUseCase,
    private val isSelfATeamMember: IsSelfATeamMemberUseCase,
    private val serverConfig: SelfServerConfigUseCase,
    private val isPasswordRequired: IsPasswordRequiredUseCase,
    private val isReadOnlyAccount: IsReadOnlyAccountUseCase,
    private val dispatchers: DispatcherProvider,
    private val isE2EIEnabledUseCase: IsE2EIEnabledUseCase,
) {
    fun create(): MyAccountViewModel = MyAccountViewModel(
        getSelf = getSelf,
        observeSelfUserWithTeam = observeSelfUserWithTeam,
        isSelfATeamMember = isSelfATeamMember,
        serverConfig = serverConfig,
        isPasswordRequired = isPasswordRequired,
        isReadOnlyAccount = isReadOnlyAccount,
        dispatchers = dispatchers,
        isE2EIEnabledUseCase = isE2EIEnabledUseCase,
    )
}
