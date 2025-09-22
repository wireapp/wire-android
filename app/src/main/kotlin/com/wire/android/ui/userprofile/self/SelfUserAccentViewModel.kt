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

package com.wire.android.ui.userprofile.self

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.theme.Accent
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelfUserAccentViewModel @Inject constructor(
    private val observeSelf: ObserveSelfUserUseCase,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    var accentState by mutableStateOf(Accent.Unknown)
        private set

    init {
        viewModelScope.launch {
            fetchSelfUser()
        }
    }

    private fun fetchSelfUser() {
        viewModelScope.launch {
            observeSelf().flowOn(dispatchers.io()).shareIn(this, SharingStarted.WhileSubscribed(1))
                .map { it.accentId }
                .distinctUntilChanged()
                .collect { accentId ->
                    accentState = Accent.fromAccentId(accentId)
                }
        }
    }
}
