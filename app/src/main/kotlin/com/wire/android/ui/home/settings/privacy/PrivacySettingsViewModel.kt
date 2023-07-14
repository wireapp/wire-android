/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.home.settings.privacy

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.feature.user.readReceipts.ObserveReadReceiptsEnabledUseCase
import com.wire.kalium.logic.feature.user.readReceipts.PersistReadReceiptsStatusConfigUseCase
import com.wire.kalium.logic.feature.user.readReceipts.ReadReceiptStatusConfigResult
import com.wire.kalium.logic.feature.user.screenshotCensoring.ObserveScreenshotCensoringConfigResult
import com.wire.kalium.logic.feature.user.screenshotCensoring.ObserveScreenshotCensoringConfigUseCase
import com.wire.kalium.logic.feature.user.screenshotCensoring.PersistScreenshotCensoringConfigResult
import com.wire.kalium.logic.feature.user.screenshotCensoring.PersistScreenshotCensoringConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@HiltViewModel
class PrivacySettingsViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val dispatchers: DispatcherProvider,
    private val persistReadReceiptsStatusConfig: PersistReadReceiptsStatusConfigUseCase,
    private val observeReadReceiptsEnabled: ObserveReadReceiptsEnabledUseCase,
    private val persistScreenshotCensoringConfig: PersistScreenshotCensoringConfigUseCase,
    private val observeScreenshotCensoringConfig: ObserveScreenshotCensoringConfigUseCase
) : ViewModel() {

    var state by mutableStateOf(PrivacySettingsState())
        private set

    init {
        viewModelScope.launch {
            combine(
                observeReadReceiptsEnabled(),
                observeScreenshotCensoringConfig(),
                ::Pair
            ).collect { (readReceiptsEnabled, screenshotCensoringConfig) ->
                state = state.copy(
                    isReadReceiptsEnabled = readReceiptsEnabled,
                    screenshotCensoringConfig = when (screenshotCensoringConfig) {
                        ObserveScreenshotCensoringConfigResult.Disabled ->
                            ScreenshotCensoringConfig.DISABLED

                        ObserveScreenshotCensoringConfigResult.Enabled.ChosenByUser ->
                            ScreenshotCensoringConfig.ENABLED_BY_USER

                        ObserveScreenshotCensoringConfigResult.Enabled.EnforcedByTeamSelfDeletingSettings ->
                            ScreenshotCensoringConfig.ENFORCED_BY_TEAM
                    }
                )
            }
        }
    }

    fun navigateBack() {
        viewModelScope.launch { navigationManager.navigateBack() }
    }

    fun setReadReceiptsState(isEnabled: Boolean) {
        viewModelScope.launch {
            when (withContext(dispatchers.io()) { persistReadReceiptsStatusConfig(isEnabled) }) {
                is ReadReceiptStatusConfigResult.Failure -> {
                    appLogger.e("Something went wrong while updating read receipts config")
                    state = state.copy(isReadReceiptsEnabled = !isEnabled)
                }
                is ReadReceiptStatusConfigResult.Success -> {
                    appLogger.d("Read receipts config changed")
                    state = state.copy(isReadReceiptsEnabled = isEnabled)
                }
            }
        }
    }

    fun setScreenshotCensoringConfig(isEnabled: Boolean) {
        viewModelScope.launch {
            when (withContext(dispatchers.io()) { persistScreenshotCensoringConfig(isEnabled) }) {
                is PersistScreenshotCensoringConfigResult.Failure -> {
                    appLogger.e("Something went wrong while updating screenshot censoring config")
                }

                is PersistScreenshotCensoringConfigResult.Success -> {
                    appLogger.d("Screenshot censoring config changed")
                }
            }
        }
    }
}
