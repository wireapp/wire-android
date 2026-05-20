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
package com.wire.android.ui.home.appLock.forgot

import com.wire.android.datastore.GlobalDataStore
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.notification.WireNotificationManager
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import dev.zacsweers.metro.Inject

@Inject
@Suppress("LongParameterList")
class ForgotLockScreenViewModelFactory(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val globalDataStore: GlobalDataStore,
    private val notificationManager: WireNotificationManager,
    private val userDataStoreProvider: UserDataStoreProvider,
    private val getSessions: GetSessionsUseCase,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val endCall: EndCallUseCase,
    private val accountSwitch: AccountSwitchUseCase,
) {
    fun create(): ForgotLockScreenViewModel = ForgotLockScreenViewModel(
        coreLogic = coreLogic,
        globalDataStore = globalDataStore,
        notificationManager = notificationManager,
        userDataStoreProvider = userDataStoreProvider,
        getSessions = getSessions,
        observeEstablishedCalls = observeEstablishedCalls,
        endCall = endCall,
        accountSwitch = accountSwitch,
    )
}
