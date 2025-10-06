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
package com.wire.android.ui.userprofile.self

import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.datastore.UserDataStore
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.framework.TestTeam
import com.wire.android.framework.TestUser
import com.wire.android.mapper.OtherAccountMapper
import com.wire.android.notification.WireNotificationManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.common.functional.Either
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.auth.LogoutUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.client.IsProfileQRCodeEnabledUseCase
import com.wire.kalium.logic.feature.legalhold.LegalHoldStateForSelfUser
import com.wire.kalium.logic.feature.legalhold.ObserveLegalHoldStateForSelfUserUseCase
import com.wire.kalium.logic.feature.personaltoteamaccount.CanMigrateFromPersonalToTeamUseCase
import com.wire.kalium.logic.feature.server.GetTeamUrlUseCase
import com.wire.kalium.logic.feature.team.GetUpdatedSelfTeamUseCase
import com.wire.kalium.logic.feature.user.IsReadOnlyAccountUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import com.wire.kalium.logic.feature.user.ObserveValidAccountsUseCase
import com.wire.kalium.logic.feature.user.UpdateSelfAvailabilityStatusUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf

class SelfUserProfileViewModelArrangement {
    @MockK
    lateinit var userDataStore: UserDataStore

    @MockK
    lateinit var getSelf: ObserveSelfUserUseCase

    @MockK
    lateinit var getSelfTeam: GetUpdatedSelfTeamUseCase

    @MockK
    lateinit var observeValidAccounts: ObserveValidAccountsUseCase

    @MockK
    lateinit var updateStatus: UpdateSelfAvailabilityStatusUseCase

    @MockK
    lateinit var logout: LogoutUseCase

    @MockK
    lateinit var observeLegalHoldStatusForSelfUser: ObserveLegalHoldStateForSelfUserUseCase

    @MockK
    lateinit var dispatchers: DispatcherProvider

    @MockK
    lateinit var otherAccountMapper: OtherAccountMapper

    @MockK
    lateinit var observeEstablishedCalls: ObserveEstablishedCallsUseCase

    @MockK
    lateinit var accountSwitch: AccountSwitchUseCase

    @MockK
    lateinit var endCall: EndCallUseCase

    @MockK
    lateinit var isReadOnlyAccount: IsReadOnlyAccountUseCase

    @MockK
    lateinit var notificationManager: WireNotificationManager

    @MockK
    lateinit var globalDataStore: GlobalDataStore

    @MockK
    lateinit var qualifiedIdMapper: QualifiedIdMapper

    @MockK
    lateinit var anonymousAnalyticsManager: AnonymousAnalyticsManager

    @MockK
    lateinit var canMigrateFromPersonalToTeam: CanMigrateFromPersonalToTeamUseCase

    @MockK
    lateinit var getTeamUrl: GetTeamUrlUseCase

    @MockK
    lateinit var profileQRCodeEnabledUseCase: IsProfileQRCodeEnabledUseCase

    private val viewModel by lazy {
        SelfUserProfileViewModel(
            selfUserId = TestUser.SELF_USER.id,
            dataStore = userDataStore,
            observeSelf = getSelf,
            getSelfTeam = getSelfTeam,
            observeValidAccounts = observeValidAccounts,
            updateStatus = updateStatus,
            logout = logout,
            observeLegalHoldStatusForSelfUser = observeLegalHoldStatusForSelfUser,
            dispatchers = TestDispatcherProvider(),
            otherAccountMapper = otherAccountMapper,
            observeEstablishedCalls = observeEstablishedCalls,
            accountSwitch = accountSwitch,
            endCall = endCall,
            isReadOnlyAccount = isReadOnlyAccount,
            notificationManager = notificationManager,
            globalDataStore = globalDataStore,
            qualifiedIdMapper = qualifiedIdMapper,
            anonymousAnalyticsManager = anonymousAnalyticsManager,
            canMigrateFromPersonalToTeam = canMigrateFromPersonalToTeam,
            getTeamUrl = getTeamUrl,
            isProfileQRCodeEnabled = profileQRCodeEnabledUseCase,
        )
    }

    init {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockUri()

        coEvery { getSelf.invoke() } returns flowOf(TestUser.SELF_USER)
        coEvery { getSelfTeam.invoke() } returns Either.Right(TestTeam.TEAM)
        coEvery { observeValidAccounts.invoke() } returns flowOf(listOf(TestUser.SELF_USER to TestTeam.TEAM))
        coEvery { isReadOnlyAccount.invoke() } returns false
        coEvery { observeEstablishedCalls.invoke() } returns flowOf(emptyList())
        coEvery { observeEstablishedCalls.invoke() } returns flowOf(emptyList())
        coEvery { canMigrateFromPersonalToTeam.invoke() } returns true
        coEvery { getTeamUrl.invoke() } returns ""
        coEvery { profileQRCodeEnabledUseCase.invoke() } returns true
    }

    fun withLegalHoldStatus(result: LegalHoldStateForSelfUser) = apply {
        coEvery { observeLegalHoldStatusForSelfUser.invoke() } returns flowOf(result)
    }

    fun arrange() = this to viewModel
}
