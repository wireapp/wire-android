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

package com.wire.android.ui.home.settings.mfa.authenticatorapps.edit

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.kalium.common.functional.fold
import com.wire.kalium.logic.feature.mfa.AuthenticatorApp
import com.wire.kalium.logic.feature.mfa.RenameAuthenticatorAppUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MFAEditAuthenticatorAppState(
    val app: AuthenticatorApp? = null,
    val isSaving: Boolean = false,
    val hasError: Boolean = false,
    val result: AuthenticatorAppRenameResult? = null,
)

class MFAEditAuthenticatorAppViewModel @Inject constructor(
    private val renameApp: RenameAuthenticatorAppUseCase,
) : ViewModel() {
    val name = TextFieldState()
    private val _state = MutableStateFlow(MFAEditAuthenticatorAppState())
    val state = _state.asStateFlow()
    private var saveJob: Job? = null

    fun initialize(app: AuthenticatorApp) {
        // Observation and configuration changes must not overwrite an in-progress draft.
        if (_state.value.app != null) return
        name.setTextAndPlaceCursorAtEnd(app.name)
        _state.update { it.copy(app = app) }
    }

    @Suppress("TooGenericExceptionCaught")
    fun save() {
        val current = _state.value
        val app = current.app ?: return
        val newName = name.text.toString().trim()
        val canSave = !current.isSaving && current.result == null
        if (!canSave || newName.isEmpty() || newName == app.name) return
        _state.update { it.copy(isSaving = true, hasError = false) }
        saveJob = viewModelScope.launch {
            try {
                renameApp(app.id, newName).fold(
                    { _state.update { it.copy(hasError = true) } },
                    { renamed -> _state.update { it.copy(result = AuthenticatorAppRenameResult(renamed.id, renamed.name)) } },
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(hasError = true) }
            } finally {
                _state.update { it.copy(isSaving = false) }
            }
        }
    }

    fun cancel() {
        saveJob?.cancel()
        _state.update { it.copy(result = null, hasError = false) }
    }
}
