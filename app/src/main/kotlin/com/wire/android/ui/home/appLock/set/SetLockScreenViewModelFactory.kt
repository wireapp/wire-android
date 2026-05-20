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
package com.wire.android.ui.home.appLock.set

import com.wire.android.datastore.GlobalDataStore
import com.wire.android.feature.ObserveAppLockConfigUseCase
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.feature.applock.MarkTeamAppLockStatusAsNotifiedUseCase
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppLockEditableUseCase
import dev.zacsweers.metro.Inject

@Inject
class SetLockScreenViewModelFactory(
    private val validatePassword: ValidatePasswordUseCase,
    private val globalDataStore: GlobalDataStore,
    private val dispatchers: DispatcherProvider,
    private val observeAppLockConfig: ObserveAppLockConfigUseCase,
    private val observeIsAppLockEditable: ObserveIsAppLockEditableUseCase,
    private val markTeamAppLockStatusAsNotified: MarkTeamAppLockStatusAsNotifiedUseCase,
) {
    fun create(): SetLockScreenViewModel = SetLockScreenViewModel(
        validatePassword = validatePassword,
        globalDataStore = globalDataStore,
        dispatchers = dispatchers,
        observeAppLockConfig = observeAppLockConfig,
        observeIsAppLockEditable = observeIsAppLockEditable,
        markTeamAppLockStatusAsNotified = markTeamAppLockStatusAsNotified,
    )
}
