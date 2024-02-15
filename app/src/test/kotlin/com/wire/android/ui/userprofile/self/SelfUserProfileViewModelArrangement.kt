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
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.framework.TestTeam
import com.wire.android.framework.TestUser
import com.wire.android.mapper.OtherAccountMapper
import com.wire.android.notification.NotificationChannelsManager
import com.wire.android.notification.WireNotificationManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.auth.LogoutUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.legalhold.LegalHoldStateForSelfUser
import com.wire.kalium.logic.feature.legalhold.ObserveLegalHoldStateForSelfUserUseCase
import com.wire.kalium.logic.feature.team.GetUpdatedSelfTeamUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.IsReadOnlyAccountUseCase
import com.wire.kalium.logic.feature.user.ObserveValidAccountsUseCase
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import com.wire.kalium.logic.feature.user.UpdateSelfAvailabilityStatusUseCase
import com.wire.kalium.logic.functional.Either
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf

class SelfUserProfileViewModelArrangement {
    @MockK
    lateinit var userDataStore: UserDataStore
    @MockK
    lateinit var getSelf: GetSelfUserUseCase
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
    lateinit var wireSessionImageLoader: WireSessionImageLoader
    @MockK
    lateinit var authServerConfigProvider: AuthServerConfigProvider
    @MockK
    lateinit var selfServerLinks: SelfServerConfigUseCase
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
    lateinit var notificationChannelsManager: NotificationChannelsManager
    @MockK
    lateinit var notificationManager: WireNotificationManager
    @MockK
    lateinit var globalDataStore: GlobalDataStore
    @MockK
    lateinit var qualifiedIdMapper: QualifiedIdMapper

    private val viewModel by lazy {
        SelfUserProfileViewModel(
            selfUserId = TestUser.SELF_USER.id,
            dataStore = userDataStore,
            getSelf = getSelf,
            getSelfTeam = getSelfTeam,
            observeValidAccounts = observeValidAccounts,
            updateStatus = updateStatus,
            logout = logout,
            observeLegalHoldStatusForSelfUser = observeLegalHoldStatusForSelfUser,
            dispatchers = TestDispatcherProvider(),
            wireSessionImageLoader = wireSessionImageLoader,
            authServerConfigProvider = authServerConfigProvider,
            selfServerLinks = selfServerLinks,
            otherAccountMapper = otherAccountMapper,
            observeEstablishedCalls = observeEstablishedCalls,
            accountSwitch = accountSwitch,
            endCall = endCall,
            isReadOnlyAccount = isReadOnlyAccount,
            notificationChannelsManager = notificationChannelsManager,
            notificationManager = notificationManager,
            globalDataStore = globalDataStore,
            qualifiedIdMapper = qualifiedIdMapper
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
    }
    fun withLegalHoldStatus(result: LegalHoldStateForSelfUser) = apply {
        coEvery { observeLegalHoldStatusForSelfUser.invoke() } returns flowOf(result)
    }
    fun arrange() = this to viewModel
}
