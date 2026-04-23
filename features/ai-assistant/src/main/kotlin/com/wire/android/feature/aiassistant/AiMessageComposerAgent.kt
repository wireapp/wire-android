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
package com.wire.android.feature.aiassistant

interface AiMessageComposerAgent {
    suspend fun proofread(inputText: String): AiMessageComposerResult
    suspend fun adjustTone(inputText: String, toneType: AiMessageToneType): AiMessageComposerResult
    suspend fun customPrompt(inputText: String, userPrompt: String): AiMessageComposerResult
}

enum class AiMessageToneType {
    Formal,
    Informal
}

sealed interface AiMessageComposerResult {
    data class Success(val updatedText: String) : AiMessageComposerResult
    data object MissingModel : AiMessageComposerResult
    data object UnsupportedModel : AiMessageComposerResult
    data object EmptyInput : AiMessageComposerResult
    data object EmptyResponse : AiMessageComposerResult
    data class InferenceFailed(val message: String) : AiMessageComposerResult
}
