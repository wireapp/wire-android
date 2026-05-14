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
package com.wire.android.ui.calling

import com.wire.android.di.ObserveScreenshotCensoringConfigUseCaseProvider
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import dev.zacsweers.metro.Inject

@Inject
class CallActivityViewModelFactory(
    private val dispatchers: DispatcherProvider,
    private val currentSession: CurrentSessionUseCase,
    private val observeScreenshotCensoringConfigUseCaseProviderFactory:
    ObserveScreenshotCensoringConfigUseCaseProvider.Factory,
    private val accountSwitch: AccountSwitchUseCase,
) {
    fun create(): CallActivityViewModel = CallActivityViewModel(
        dispatchers = dispatchers,
        currentSession = currentSession,
        observeScreenshotCensoringConfigUseCaseProviderFactory = observeScreenshotCensoringConfigUseCaseProviderFactory,
        accountSwitch = accountSwitch,
    )
}
