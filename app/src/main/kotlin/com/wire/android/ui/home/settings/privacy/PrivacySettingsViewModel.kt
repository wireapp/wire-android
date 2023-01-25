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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PrivacySettingsViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val dispatchers: DispatcherProvider,
    private val persistReadReceiptsStatusConfig: PersistReadReceiptsStatusConfigUseCase,
    private val observeReadReceiptsEnabled: ObserveReadReceiptsEnabledUseCase,
) : ViewModel() {

    var state by mutableStateOf(PrivacySettingsState())
        private set

    init {
        viewModelScope.launch {
            observeReadReceiptsEnabled().collect {
                state = state.copy(isReadReceiptsEnabled = it)
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
}
