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
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.ui.home.messagecomposer

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.feature.aiassistant.AiMessageComposerAgent
import com.wire.android.feature.aiassistant.AiMessageComposerResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AiMessageComposerViewModel @Inject constructor(
    private val aiMessageComposerAgent: AiMessageComposerAgent
) : ViewModel() {

    var isProofreading by mutableStateOf(false)
        private set

    private val _effect = MutableSharedFlow<AiMessageComposerEffect>()
    val effect: SharedFlow<AiMessageComposerEffect> = _effect.asSharedFlow()

    fun proofread(inputText: String) {
        if (isProofreading) return

        viewModelScope.launch {
            try {
                isProofreading = true
                when (val result = aiMessageComposerAgent.proofread(inputText)) {
                    is AiMessageComposerResult.Success -> _effect.emit(
                        AiMessageComposerEffect.ReplaceText(result.updatedText)
                    )
                    AiMessageComposerResult.EmptyInput -> _effect.emit(
                        AiMessageComposerEffect.ShowError(R.string.error_proofread_message_empty_input)
                    )
                    AiMessageComposerResult.MissingModel -> _effect.emit(
                        AiMessageComposerEffect.ShowError(R.string.error_proofread_message_missing_model)
                    )
                    AiMessageComposerResult.UnsupportedModel -> _effect.emit(
                        AiMessageComposerEffect.ShowError(R.string.error_proofread_message_unsupported_model)
                    )
                    AiMessageComposerResult.EmptyResponse,
                    is AiMessageComposerResult.InferenceFailed -> _effect.emit(
                        AiMessageComposerEffect.ShowError(R.string.error_proofread_message_generic)
                    )
                }
            } finally {
                isProofreading = false
            }
        }
    }
}

sealed interface AiMessageComposerEffect {
    data class ReplaceText(val updatedText: String) : AiMessageComposerEffect
    data class ShowError(@param:StringRes val messageResId: Int) : AiMessageComposerEffect
}
