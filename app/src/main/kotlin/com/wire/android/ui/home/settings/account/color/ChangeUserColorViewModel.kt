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

package com.wire.android.ui.home.settings.account.color

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.common.ActionsViewModel
import com.wire.android.ui.theme.Accent
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.UpdateAccentColorResult
import com.wire.kalium.logic.feature.user.UpdateAccentColorUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChangeUserColorViewModel @Inject constructor(
    private val getSelf: GetSelfUserUseCase,
    private val updateAccentColor: UpdateAccentColorUseCase,
) : ActionsViewModel<ChangeUserColorAction>() {

    var accentState: AccentActionState by mutableStateOf(AccentActionState(null))
        private set

    init {
        viewModelScope.launch {
            getSelf()?.accentId.let { accentId ->
                accentState = AccentActionState(
                    accent = Accent.fromAccentId(accentId)
                )
            }
        }
    }

    fun changeAccentColor(accent: Accent) {
        accentState = accentState.copy(accent = accent)
    }

    fun saveAccentColor() {
        viewModelScope.launch {
            accentState.accent?.let { accent ->
                accentState.copy(isPerformingAction = true)
                updateAccentColor(accent.accentId)
                    .let {
                        accentState = accentState.copy(isPerformingAction = false)
                        sendAction(
                            when (it) {
                                is UpdateAccentColorResult.Failure -> ChangeUserColorAction.Failure
                                UpdateAccentColorResult.Success -> ChangeUserColorAction.Success
                            }
                        )
                    }
            }
        }
    }
}

data class AccentActionState(
    val accent: Accent?,
    val isPerformingAction: Boolean = false,
)

enum class ChangeUserColorAction {
    Success,
    Failure
}
