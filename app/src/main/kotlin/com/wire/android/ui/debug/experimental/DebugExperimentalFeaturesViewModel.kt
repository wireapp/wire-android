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
package com.wire.android.ui.debug.experimental

import androidx.lifecycle.ViewModel
import com.wire.android.util.debug.ExperimentalFeature
import com.wire.android.util.debug.ExperimentalFeaturesStore
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DebugExperimentalFeaturesViewModel @Inject constructor(
    private val experimentalFeatures: ExperimentalFeaturesStore,
) : ViewModel() {

    private val _state = MutableStateFlow(DebugExperimentalFeaturesState())
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    fun setEnabled(key: String, enabled: Boolean) {
        experimentalFeatures.setEnabled(key, enabled)
        refresh()
    }

    fun resetAll() {
        experimentalFeatures.resetAll()
        refresh()
    }

    private fun refresh() {
        _state.update {
            it.copy(
                features = experimentalFeatures.all(),
                isRestartRequired = experimentalFeatures.isRestartRequired(),
            )
        }
    }
}

data class DebugExperimentalFeaturesState(
    val features: List<ExperimentalFeature> = emptyList(),
    val isRestartRequired: Boolean = false,
)