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
package com.wire.android.ui.debug.automaticbackups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.feature.backup.BackupRootKeyInfo
import com.wire.kalium.logic.feature.backup.GenerateBackupRootKeyResult
import com.wire.kalium.logic.feature.backup.GenerateBackupRootKeyUseCase
import com.wire.kalium.logic.feature.backup.GetBackupRootKeyResult
import com.wire.kalium.logic.feature.backup.GetBackupRootKeyUseCase
import com.wire.kalium.logic.feature.backup.toBackupRootKeyInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AutomaticBackupsDebugViewModel(
    private val getBackupRootKey: GetBackupRootKeyUseCase,
    private val generateBackupRootKey: GenerateBackupRootKeyUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AutomaticBackupsDebugState())
    val state: StateFlow<AutomaticBackupsDebugState> = _state.asStateFlow()

    private val _infoMessage = MutableSharedFlow<UIText>()
    val infoMessage: SharedFlow<UIText> = _infoMessage.asSharedFlow()

    init {
        loadBackupRootKey()
    }

    fun loadBackupRootKey() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = getBackupRootKey()) {
                is GetBackupRootKeyResult.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            backupRootKey = result.backupRootKey?.toBackupRootKeyInfo(),
                        )
                    }
                }
                is GetBackupRootKeyResult.Failure -> {
                    _state.update { it.copy(isLoading = false) }
                    _infoMessage.emit(UIText.DynamicString("Failed to load Backup Root Key: ${result.cause.message.orEmpty()}"))
                }
            }
        }
    }

    fun generateNewBackupRootKey() {
        viewModelScope.launch {
            _state.update { it.copy(isGenerating = true) }
            when (val result = generateBackupRootKey()) {
                is GenerateBackupRootKeyResult.Success -> {
                    _state.update {
                        it.copy(
                            isGenerating = false,
                            backupRootKey = result.backupRootKey.toBackupRootKeyInfo(),
                        )
                    }
                    _infoMessage.emit(UIText.DynamicString("Backup Root Key generated"))
                }
                is GenerateBackupRootKeyResult.Failure.CurrentClientIdUnavailable -> {
                    _state.update { it.copy(isGenerating = false) }
                    _infoMessage.emit(UIText.DynamicString("Failed to get current client ID: ${result.cause}"))
                }
                is GenerateBackupRootKeyResult.Failure.StorageFailure -> {
                    _state.update { it.copy(isGenerating = false) }
                    _infoMessage.emit(UIText.DynamicString("Failed to generate Backup Root Key: ${result.cause.message.orEmpty()}"))
                }
            }
        }
    }
}

data class AutomaticBackupsDebugState(
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val backupRootKey: BackupRootKeyInfo? = null,
)
