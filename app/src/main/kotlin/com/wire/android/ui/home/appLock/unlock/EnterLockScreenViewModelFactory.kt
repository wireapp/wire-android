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
package com.wire.android.ui.home.appLock.unlock

import com.wire.android.datastore.GlobalDataStore
import com.wire.android.ui.home.appLock.LockCodeTimeManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import dev.zacsweers.metro.Inject

@Inject
class EnterLockScreenViewModelFactory(
    private val validatePassword: ValidatePasswordUseCase,
    private val globalDataStore: GlobalDataStore,
    private val dispatchers: DispatcherProvider,
    private val lockCodeTimeManager: LockCodeTimeManager,
) {
    fun create(): EnterLockScreenViewModel = EnterLockScreenViewModel(
        validatePassword = validatePassword,
        globalDataStore = globalDataStore,
        dispatchers = dispatchers,
        lockCodeTimeManager = lockCodeTimeManager,
    )
}
