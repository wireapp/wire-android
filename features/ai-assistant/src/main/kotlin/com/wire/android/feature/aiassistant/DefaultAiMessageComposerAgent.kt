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

import com.wire.android.feature.aiassistant.model.AiModelStatus
import com.wire.android.feature.aiassistant.test.LiteRtLmInferenceFactory
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class DefaultAiMessageComposerAgent @Inject constructor(
    private val aiModelManager: AiModelManager,
    private val inferenceFactory: LiteRtLmInferenceFactory
) : AiMessageComposerAgent {

    override suspend fun proofread(inputText: String): AiMessageComposerResult =
        generateUpdatedMessage(
            inputText = inputText,
            userMessage = "Fix grammar, return one result only:\n\"$inputText\"",
            initialExchanges = PROOFREAD_EXAMPLES
        )

    override suspend fun adjustTone(inputText: String, toneType: AiMessageToneType): AiMessageComposerResult {
        val instruction = toneType.toInstruction()
        return generateUpdatedMessage(
            inputText = inputText,
            userMessage = "$instruction, return one result only:\n\"$inputText\"",
            initialExchanges = toneType.toExamples()
        )
    }

    override suspend fun customPrompt(inputText: String, userPrompt: String): AiMessageComposerResult =
        generateUpdatedMessage(
            inputText = inputText,
            userMessage = "$userPrompt, return one result only:\n\"$inputText\"",
            initialExchanges = emptyList()
        )

    private suspend fun generateUpdatedMessage(
        inputText: String,
        userMessage: String,
        initialExchanges: List<Pair<String, String>>
    ): AiMessageComposerResult =
        withContext(Dispatchers.IO) {
            if (inputText.isBlank()) {
                return@withContext AiMessageComposerResult.EmptyInput
            }

            val modelStatus = aiModelManager.observeModelStatus().first()
            if (modelStatus !is AiModelStatus.Ready) {
                return@withContext AiMessageComposerResult.MissingModel
            }
            if (!modelStatus.localPath.endsWith(LITERT_LM_EXTENSION, ignoreCase = true)) {
                return@withContext AiMessageComposerResult.UnsupportedModel
            }

            runCatching {
                inferenceFactory.create(modelStatus.localPath, initialExchanges).use { inference ->
                    inference.generateResponse(userMessage)
                }
            }.fold(
                onSuccess = { response ->
                    val trimmed = response.trimMatchingQuotes()
                    if (trimmed.isBlank()) {
                        AiMessageComposerResult.EmptyResponse
                    } else {
                        AiMessageComposerResult.Success(trimmed)
                    }
                },
                onFailure = { throwable ->
                    if (throwable is CancellationException) throw throwable
                    AiMessageComposerResult.InferenceFailed(throwable.message ?: throwable::class.java.simpleName)
                }
            )
        }

    private companion object {
        const val LITERT_LM_EXTENSION = ".litertlm"

        fun String.trimMatchingQuotes(): String {
            if (length < 2) return this
            return when {
                first() == '"' && last() == '"' -> substring(1, length - 1)
                first() == '\u201C' && last() == '\u201D' -> substring(1, length - 1) // " "
                first() == '\'' && last() == '\'' -> substring(1, length - 1)
                else -> this
            }
        }

        val PROOFREAD_EXAMPLES = listOf(
            "Fix grammar, return one result only:\n\"She dont know nothing about it.\"" to
                "She doesn't know anything about it."
        )

        fun AiMessageToneType.toInstruction(): String = when (this) {
            AiMessageToneType.Formal -> "Rewrite more formally"
            AiMessageToneType.Informal -> "Rewrite more casually"
        }

        fun AiMessageToneType.toExamples(): List<Pair<String, String>> = when (this) {
            AiMessageToneType.Formal -> listOf(
                "Rewrite more formally, return one result only:\n\"Hey can u help me with this?\"" to
                    "Could you please assist me with this?"
            )
            AiMessageToneType.Informal -> listOf(
                "Rewrite more casually, return one result only:\n\"I would like to request your assistance with this matter.\"" to
                    "Can you help me out with this?"
            )
        }
    }
}
