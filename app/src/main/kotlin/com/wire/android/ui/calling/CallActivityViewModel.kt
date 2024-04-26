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
package com.wire.android.ui.calling

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.ObserveScreenshotCensoringConfigUseCaseProvider
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.feature.SwitchAccountParam
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.feature.user.screenshotCensoring.ObserveScreenshotCensoringConfigResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CallActivityViewModel @Inject constructor(
    private val dispatchers: DispatcherProvider,
    private val currentSession: CurrentSessionUseCase,
    private val observeScreenshotCensoringConfigUseCaseProviderFactory:
    ObserveScreenshotCensoringConfigUseCaseProvider.Factory,
    private val accountSwitch: AccountSwitchUseCase
) : ViewModel() {

    fun isScreenshotCensoringConfigEnabled(): Deferred<Boolean> =
        viewModelScope.async(dispatchers.io()) {
            val currentSession = currentSession()
            if (currentSession is CurrentSessionResult.Success) {
                return@async observeScreenshotCensoringConfigUseCaseProviderFactory.create(
                    currentSession.accountInfo.userId
                ).observeScreenshotCensoringConfig().map {
                    it is ObserveScreenshotCensoringConfigResult.Enabled
                }.first()
            } else {
                return@async false
            }
        }

    fun switchAccountIfNeeded(userId: UserId) {
        viewModelScope.launch(Dispatchers.IO) {
            val shouldSwitchAccount = when (val result = currentSession()) {
                is CurrentSessionResult.Failure.Generic -> true
                CurrentSessionResult.Failure.SessionNotFound -> true
                is CurrentSessionResult.Success -> result.accountInfo.userId != userId
            }
            if (shouldSwitchAccount) {
                accountSwitch(SwitchAccountParam.SwitchToAccount(userId))
            }
        }
    }
}
