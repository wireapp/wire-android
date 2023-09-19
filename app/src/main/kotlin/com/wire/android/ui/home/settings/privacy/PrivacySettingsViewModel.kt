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
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.feature.user.readReceipts.ObserveReadReceiptsEnabledUseCase
import com.wire.kalium.logic.feature.user.readReceipts.PersistReadReceiptsStatusConfigUseCase
import com.wire.kalium.logic.feature.user.readReceipts.ReadReceiptStatusConfigResult
import com.wire.kalium.logic.feature.user.screenshotCensoring.ObserveScreenshotCensoringConfigResult
import com.wire.kalium.logic.feature.user.screenshotCensoring.ObserveScreenshotCensoringConfigUseCase
import com.wire.kalium.logic.feature.user.screenshotCensoring.PersistScreenshotCensoringConfigResult
import com.wire.kalium.logic.feature.user.screenshotCensoring.PersistScreenshotCensoringConfigUseCase
import com.wire.kalium.logic.feature.user.typingIndicator.ObserveTypingIndicatorEnabledUseCase
import com.wire.kalium.logic.feature.user.typingIndicator.PersistTypingIndicatorStatusConfigUseCase
import com.wire.kalium.logic.feature.user.typingIndicator.TypingIndicatorConfigResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PrivacySettingsViewModel @Inject constructor(
    private val dispatchers: DispatcherProvider,
    private val persistReadReceiptsStatusConfig: PersistReadReceiptsStatusConfigUseCase,
    private val observeReadReceiptsEnabled: ObserveReadReceiptsEnabledUseCase,
    private val persistScreenshotCensoringConfig: PersistScreenshotCensoringConfigUseCase,
    private val observeScreenshotCensoringConfig: ObserveScreenshotCensoringConfigUseCase,
    private val persistTypingIndicatorStatusConfig: PersistTypingIndicatorStatusConfigUseCase,
    private val observeTypingIndicatorEnabled: ObserveTypingIndicatorEnabledUseCase
) : ViewModel() {

    var state by mutableStateOf(PrivacySettingsState())
        private set

    init {
        viewModelScope.launch {
            combine(
                observeReadReceiptsEnabled(),
                observeTypingIndicatorEnabled(),
                observeScreenshotCensoringConfig(),
                ::Triple
            ).collect { (readReceiptsEnabled, typingIndicatorEnabled, screenshotCensoringConfig) ->
                state = state.copy(
                    areReadReceiptsEnabled = readReceiptsEnabled,
                    isTypingIndicatorEnabled = typingIndicatorEnabled,
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

    fun setReadReceiptsState(isEnabled: Boolean) {
        viewModelScope.launch {
            state =
                when (withContext(dispatchers.io()) { persistReadReceiptsStatusConfig(isEnabled) }) {
                    is ReadReceiptStatusConfigResult.Failure -> {
                        appLogger.e("Something went wrong while updating read receipts config")
                        state.copy(areReadReceiptsEnabled = !isEnabled)
                    }

                    is ReadReceiptStatusConfigResult.Success -> {
                        appLogger.d("Read receipts config changed")
                        state.copy(areReadReceiptsEnabled = isEnabled)
                    }
                }
        }
    }

    fun setTypingIndicatorState(isEnabled: Boolean) {
        viewModelScope.launch {
            state =
                when (withContext(dispatchers.io()) { persistTypingIndicatorStatusConfig(isEnabled) }) {
                    is TypingIndicatorConfigResult.Failure -> {
                        appLogger.e("Something went wrong while updating typing indicator config")
                        state.copy(isTypingIndicatorEnabled = !isEnabled)
                    }

                    is TypingIndicatorConfigResult.Success -> {
                        appLogger.d("Typing indicator configuration changed successfully")
                        state.copy(isTypingIndicatorEnabled = isEnabled)
                    }
                }
        }
    }

    fun setScreenshotCensoringConfig(isEnabled: Boolean) {
        viewModelScope.launch {
            when (persistScreenshotCensoringConfig(isEnabled)) {
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
