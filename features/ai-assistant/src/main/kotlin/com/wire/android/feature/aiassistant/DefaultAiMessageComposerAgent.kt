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
        generateUpdatedMessage(inputText) { it.toProofreadPrompt() }

    override suspend fun adjustTone(inputText: String, toneType: AiMessageToneType): AiMessageComposerResult =
        generateUpdatedMessage(inputText) { it.toAdjustTonePrompt(toneType) }

    private suspend fun generateUpdatedMessage(
        inputText: String,
        promptFactory: (String) -> String
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
                inferenceFactory.create(modelStatus.localPath).use { inference ->
                    inference.generateResponse(promptFactory(inputText))
                }
            }.fold(
                onSuccess = { response ->
                    if (response.isBlank()) {
                        AiMessageComposerResult.EmptyResponse
                    } else {
                        AiMessageComposerResult.Success(response)
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

        fun String.toProofreadPrompt(): String =
            """
            Proofread the following message. Ensure the message is clear and easy to read. 
            Return only the corrected message text.
            Do not add explanations, comments, labels, or quotation marks.

            Message:
            $this
            """.trimIndent()

        fun String.toAdjustTonePrompt(toneType: AiMessageToneType): String {
            val toneInstruction = when (toneType) {
                AiMessageToneType.Formal -> "more formal"
                AiMessageToneType.Informal -> "more informal"
            }
            return """
            Rewrite the following message to make its tone $toneInstruction.
            Preserve the original meaning and language.
            Return only the rewritten message text.
            Do not add explanations, comments, labels, or quotation marks.

            Message:
            $this
            """.trimIndent()
        }
    }
}
