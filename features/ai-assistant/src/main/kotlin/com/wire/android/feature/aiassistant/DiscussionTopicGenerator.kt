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
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

interface DiscussionTopicGenerator {
    suspend fun generateTopic(messages: List<DiscussionTopicMessage>): DiscussionTopicResult

    suspend fun generateTopics(messageClusters: List<List<DiscussionTopicMessage>>): List<DiscussionTopicResult> =
        messageClusters.map { generateTopic(it) }
}

data class DiscussionTopicMessage(
    val senderName: String,
    val text: String
)

sealed interface DiscussionTopicResult {
    data class Success(val topic: String) : DiscussionTopicResult
    data object MissingModel : DiscussionTopicResult
    data object UnsupportedModel : DiscussionTopicResult
    data object EmptyInput : DiscussionTopicResult
    data object EmptyResponse : DiscussionTopicResult
    data class InferenceFailed(val message: String) : DiscussionTopicResult
}

class DefaultDiscussionTopicGenerator @Inject constructor(
    private val aiModelManager: AiModelManager,
    private val inferenceConfigStore: AiInferenceConfigStore,
    private val inferenceFactory: LiteRtLmInferenceFactory
) : DiscussionTopicGenerator {

    override suspend fun generateTopic(messages: List<DiscussionTopicMessage>): DiscussionTopicResult =
        generateTopics(listOf(messages)).firstOrNull() ?: DiscussionTopicResult.EmptyInput

    override suspend fun generateTopics(messageClusters: List<List<DiscussionTopicMessage>>): List<DiscussionTopicResult> =
        withContext(Dispatchers.IO) {
            val promptMessageClusters = messageClusters.map { messages ->
                messages
                    .filter { it.text.isNotBlank() }
                    .take(MAX_MESSAGES)
            }
            val results = MutableList<DiscussionTopicResult?>(promptMessageClusters.size) { null }
            promptMessageClusters.forEachIndexed { index, messages ->
                if (messages.isEmpty()) {
                    results[index] = DiscussionTopicResult.EmptyInput
                }
            }
            val nonEmptyClusters = promptMessageClusters.withIndex().filter { it.value.isNotEmpty() }
            if (nonEmptyClusters.isEmpty()) {
                return@withContext results.map { it ?: DiscussionTopicResult.EmptyInput }
            }

            val modelStatus = aiModelManager.observeModelStatus().first()
            if (modelStatus !is AiModelStatus.Ready) {
                return@withContext results.fillMissing(DiscussionTopicResult.MissingModel)
            }
            if (!modelStatus.localPath.endsWith(LITERT_LM_EXTENSION, ignoreCase = true)) {
                return@withContext results.fillMissing(DiscussionTopicResult.UnsupportedModel)
            }

            val inferenceConfig = inferenceConfigStore.observeConfig().first()
            runCatching {
                inferenceFactory.create(modelStatus.localPath, inferenceConfig).use { inference ->
                    nonEmptyClusters.forEach { (index, messages) ->
                        results[index] = runCatching {
                            inference.generateResponse(messages.toPrompt()).toTopicResult()
                        }.getOrElse { throwable ->
                            if (throwable is CancellationException) throw throwable
                            DiscussionTopicResult.InferenceFailed(throwable.message ?: throwable::class.java.simpleName)
                        }
                    }
                }
            }.fold(
                onSuccess = { results.map { it ?: DiscussionTopicResult.EmptyResponse } },
                onFailure = { throwable ->
                    if (throwable is CancellationException) throw throwable
                    results.fillMissing(DiscussionTopicResult.InferenceFailed(throwable.message ?: throwable::class.java.simpleName))
                }
            )
        }

    private fun List<DiscussionTopicMessage>.toPrompt(): String {
        val transcript = first().let { message ->
            "${message.senderName}: ${message.text.replace('\n', ' ')}"
        }
//        val transcript = joinToString(separator = "\n") { message ->
//            "${message.senderName}: ${message.text.replace('\n', ' ')}"
//        }
        return """
            Identify the main discussion topic in this chat transcript.
            Return a concise topic title only, maximum 8 words.
            Do not include quotes, punctuation-only labels, or explanations.

            Transcript:
            $transcript
        """.trimIndent()
    }

    private fun String.toTopicResult(): DiscussionTopicResult {
        val topic = trim().trimMatchingQuotes()
        return if (topic.isBlank()) {
            DiscussionTopicResult.EmptyResponse
        } else {
            DiscussionTopicResult.Success(topic.lineSequence().first().take(MAX_TOPIC_LENGTH))
        }
    }

    private fun MutableList<DiscussionTopicResult?>.fillMissing(
        result: DiscussionTopicResult
    ): List<DiscussionTopicResult> = map { it ?: result }

    private companion object {
        const val LITERT_LM_EXTENSION = ".litertlm"
        const val MAX_MESSAGES = 80
        const val MAX_TOPIC_LENGTH = 120

        fun String.trimMatchingQuotes(): String {
            if (length < 2) return this
            return when {
                first() == '"' && last() == '"' -> substring(1, length - 1)
                first() == '\u201C' && last() == '\u201D' -> substring(1, length - 1)
                first() == '\'' && last() == '\'' -> substring(1, length - 1)
                else -> this
            }
        }
    }
}
