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

package com.wire.android.ui.home.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.GlobalDataStore
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppLockEditableUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val globalDataStore: GlobalDataStore,
    private val observeIsAppLockEditable: ObserveIsAppLockEditableUseCase
) : ViewModel() {
    var state by mutableStateOf(SettingsState())
        private set

    init {
        viewModelScope.launch {
            combine(
                observeIsAppLockEditable(),
                globalDataStore.isAppLockPasscodeSetFlow()
            ) {
                isAppLockEditable, isAppLockEnabled ->
                SettingsState(
                    isAppLockEditable = isAppLockEditable,
                    isAppLockEnabled = isAppLockEnabled
                )
            }.collect { state = it }
        }
    }

    fun disableAppLock() {
        viewModelScope.launch {
            globalDataStore.clearAppLockPasscode()
        }
    }
}
