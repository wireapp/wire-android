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
import com.wire.android.feature.aiassistant.AiMessageToneType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class AiMessageComposerViewModel(
    private val aiMessageComposerAgent: AiMessageComposerAgent
) : ViewModel() {

    var activeAction by mutableStateOf<AiMessageComposerAction?>(null)
        private set

    var canUndo by mutableStateOf(false)
        private set

    private val undoStack = mutableListOf<String>()

    private val _effect = MutableSharedFlow<AiMessageComposerEffect>()
    val effect: SharedFlow<AiMessageComposerEffect> = _effect.asSharedFlow()

    fun proofread(inputText: String) {
        runAiAction(AiMessageComposerAction.Proofread, inputText) {
            aiMessageComposerAgent.proofread(inputText)
        }
    }

    fun adjustTone(inputText: String, toneType: AiMessageToneType) {
        runAiAction(toneType.toAction(), inputText) {
            aiMessageComposerAgent.adjustTone(inputText, toneType)
        }
    }

    fun customPrompt(inputText: String, userPrompt: String) {
        runAiAction(AiMessageComposerAction.CustomPrompt, inputText) {
            aiMessageComposerAgent.customPrompt(inputText, userPrompt)
        }
    }

    fun undo() {
        val previousText = undoStack.removeLastOrNull() ?: return
        canUndo = undoStack.isNotEmpty()
        viewModelScope.launch {
            _effect.emit(AiMessageComposerEffect.ReplaceText(previousText))
        }
    }

    private fun runAiAction(
        action: AiMessageComposerAction,
        inputText: String,
        request: suspend () -> AiMessageComposerResult
    ) {
        if (activeAction != null) return

        viewModelScope.launch {
            try {
                activeAction = action
                when (val result = request()) {
                    is AiMessageComposerResult.Success -> {
                        undoStack.add(inputText)
                        canUndo = true
                        _effect.emit(AiMessageComposerEffect.ReplaceText(result.updatedText))
                    }
                    AiMessageComposerResult.EmptyInput -> _effect.emit(
                        AiMessageComposerEffect.ShowError(action.emptyInputErrorResId)
                    )
                    AiMessageComposerResult.MissingModel -> _effect.emit(
                        AiMessageComposerEffect.ShowError(action.missingModelErrorResId)
                    )
                    AiMessageComposerResult.UnsupportedModel -> _effect.emit(
                        AiMessageComposerEffect.ShowError(action.unsupportedModelErrorResId)
                    )
                    AiMessageComposerResult.EmptyResponse,
                    is AiMessageComposerResult.InferenceFailed -> _effect.emit(
                        AiMessageComposerEffect.ShowError(action.genericErrorResId)
                    )
                }
            } finally {
                activeAction = null
            }
        }
    }
}

enum class AiMessageComposerAction(
    @param:StringRes val emptyInputErrorResId: Int,
    @param:StringRes val missingModelErrorResId: Int,
    @param:StringRes val unsupportedModelErrorResId: Int,
    @param:StringRes val genericErrorResId: Int
) {
    Proofread(
        emptyInputErrorResId = R.string.error_proofread_message_empty_input,
        missingModelErrorResId = R.string.error_proofread_message_missing_model,
        unsupportedModelErrorResId = R.string.error_proofread_message_unsupported_model,
        genericErrorResId = R.string.error_proofread_message_generic
    ),
    FormalTone(
        emptyInputErrorResId = R.string.error_adjust_tone_message_empty_input,
        missingModelErrorResId = R.string.error_adjust_tone_message_missing_model,
        unsupportedModelErrorResId = R.string.error_adjust_tone_message_unsupported_model,
        genericErrorResId = R.string.error_adjust_tone_message_generic
    ),
    InformalTone(
        emptyInputErrorResId = R.string.error_adjust_tone_message_empty_input,
        missingModelErrorResId = R.string.error_adjust_tone_message_missing_model,
        unsupportedModelErrorResId = R.string.error_adjust_tone_message_unsupported_model,
        genericErrorResId = R.string.error_adjust_tone_message_generic
    ),
    CustomPrompt(
        emptyInputErrorResId = R.string.error_custom_prompt_message_empty_input,
        missingModelErrorResId = R.string.error_custom_prompt_message_missing_model,
        unsupportedModelErrorResId = R.string.error_custom_prompt_message_unsupported_model,
        genericErrorResId = R.string.error_custom_prompt_message_generic
    )
}

private fun AiMessageToneType.toAction(): AiMessageComposerAction =
    when (this) {
        AiMessageToneType.Formal -> AiMessageComposerAction.FormalTone
        AiMessageToneType.Informal -> AiMessageComposerAction.InformalTone
    }

sealed interface AiMessageComposerEffect {
    data class ReplaceText(val updatedText: String) : AiMessageComposerEffect
    data class ShowError(@param:StringRes val messageResId: Int) : AiMessageComposerEffect
}
