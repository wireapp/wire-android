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
package com.wire.android.ui.userprofile.self

import com.wire.android.datastore.GlobalDataStore
import com.wire.android.datastore.UserDataStore
import com.wire.android.di.CurrentAccount
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.mapper.OtherAccountMapper
import com.wire.android.notification.WireNotificationManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.LogoutUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.client.IsProfileQRCodeEnabledUseCase
import com.wire.kalium.logic.feature.legalhold.ObserveLegalHoldStateForSelfUserUseCase
import com.wire.kalium.logic.feature.personaltoteamaccount.CanMigrateFromPersonalToTeamUseCase
import com.wire.kalium.logic.feature.server.GetTeamUrlUseCase
import com.wire.kalium.logic.feature.team.SyncSelfTeamInfoUseCase
import com.wire.kalium.logic.feature.user.IsReadOnlyAccountUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserWithTeamUseCase
import com.wire.kalium.logic.feature.user.ObserveValidAccountsUseCase
import com.wire.kalium.logic.feature.user.UpdateSelfAvailabilityStatusUseCase
import dev.zacsweers.metro.Inject

@Inject
@Suppress("LongParameterList")
class SelfUserProfileViewModelFactory(
    @CurrentAccount private val selfUserId: UserId,
    private val dataStore: UserDataStore,
    private val observeSelf: ObserveSelfUserUseCase,
    private val observeSelfUserWithTeam: ObserveSelfUserWithTeamUseCase,
    private val syncSelfTeamInfo: SyncSelfTeamInfoUseCase,
    private val canMigrateFromPersonalToTeam: CanMigrateFromPersonalToTeamUseCase,
    private val observeValidAccounts: ObserveValidAccountsUseCase,
    private val updateStatus: UpdateSelfAvailabilityStatusUseCase,
    private val logout: LogoutUseCase,
    private val observeLegalHoldStatusForSelfUser: ObserveLegalHoldStateForSelfUserUseCase,
    private val dispatchers: DispatcherProvider,
    private val otherAccountMapper: OtherAccountMapper,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val accountSwitch: AccountSwitchUseCase,
    private val endCall: EndCallUseCase,
    private val isReadOnlyAccount: IsReadOnlyAccountUseCase,
    private val notificationManager: WireNotificationManager,
    private val globalDataStore: GlobalDataStore,
    private val qualifiedIdMapper: QualifiedIdMapper,
    private val anonymousAnalyticsManager: AnonymousAnalyticsManager,
    private val getTeamUrl: GetTeamUrlUseCase,
    private val isProfileQRCodeEnabled: IsProfileQRCodeEnabledUseCase,
) {
    fun create(): SelfUserProfileViewModel = SelfUserProfileViewModel(
        selfUserId = selfUserId,
        dataStore = dataStore,
        observeSelf = observeSelf,
        observeSelfUserWithTeam = observeSelfUserWithTeam,
        syncSelfTeamInfo = syncSelfTeamInfo,
        canMigrateFromPersonalToTeam = canMigrateFromPersonalToTeam,
        observeValidAccounts = observeValidAccounts,
        updateStatus = updateStatus,
        logout = logout,
        observeLegalHoldStatusForSelfUser = observeLegalHoldStatusForSelfUser,
        dispatchers = dispatchers,
        otherAccountMapper = otherAccountMapper,
        observeEstablishedCalls = observeEstablishedCalls,
        accountSwitch = accountSwitch,
        endCall = endCall,
        isReadOnlyAccount = isReadOnlyAccount,
        notificationManager = notificationManager,
        globalDataStore = globalDataStore,
        qualifiedIdMapper = qualifiedIdMapper,
        anonymousAnalyticsManager = anonymousAnalyticsManager,
        getTeamUrl = getTeamUrl,
        isProfileQRCodeEnabled = isProfileQRCodeEnabled,
    )
}
