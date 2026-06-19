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
import com.wire.android.feature.aiassistant.model.AiInferenceTarget
import com.wire.android.feature.aiassistant.test.LiteRtLmInferenceFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

interface DiscussionTopicGenerator {
    suspend fun generateTopic(messages: List<DiscussionTopicMessage>): DiscussionTopicResult

    fun generateTopics(
        messageClusters: List<List<DiscussionTopicMessage>>
    ): Flow<IndexedValue<DiscussionTopicResult>> =
        flow {
            messageClusters.forEachIndexed { index, messages ->
                emit(IndexedValue(index, generateTopic(messages)))
            }
        }
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
    private val inferenceFactory: LiteRtLmInferenceFactory,
    private val wireLlmClient: WireLlmClient = UnsupportedWireLlmClient
) : DiscussionTopicGenerator {

    override suspend fun generateTopic(messages: List<DiscussionTopicMessage>): DiscussionTopicResult =
        generateTopics(listOf(messages)).first().value

    override fun generateTopics(
        messageClusters: List<List<DiscussionTopicMessage>>
    ): Flow<IndexedValue<DiscussionTopicResult>> = flow {
        val promptMessageClusters = messageClusters.map { messages ->
            messages
                .filter { it.text.isNotBlank() }
//                .take(MAX_PROMPT_MESSAGES)
        }
        if (promptMessageClusters.isEmpty()) return@flow

        val modelStatus = aiModelManager.observeModelStatus().first()
        if (modelStatus !is AiModelStatus.Ready) {
            promptMessageClusters.forEachIndexed { index, messages ->
                emit(
                    IndexedValue(
                        index,
                        if (messages.isEmpty()) DiscussionTopicResult.EmptyInput else DiscussionTopicResult.MissingModel
                    )
                )
            }
            return@flow
        }
        when (val target = modelStatus.target) {
            is AiInferenceTarget.OnDevice -> {
                if (!target.modelPath.endsWith(LITERT_LM_EXTENSION, ignoreCase = true)) {
                    promptMessageClusters.forEachIndexed { index, messages ->
                        emit(
                            IndexedValue(
                                index,
                                if (messages.isEmpty()) {
                                    DiscussionTopicResult.EmptyInput
                                } else {
                                    DiscussionTopicResult.UnsupportedModel
                                }
                            )
                        )
                    }
                    return@flow
                }
                val inferenceConfig = inferenceConfigStore.observeConfig().first()
                val inference = try {
                    inferenceFactory.create(target.modelPath, inferenceConfig)
                } catch (throwable: Throwable) {
                    if (throwable is CancellationException) throw throwable
                    val failure = DiscussionTopicResult.InferenceFailed(
                        throwable.message ?: throwable::class.java.simpleName
                    )
                    promptMessageClusters.forEachIndexed { index, messages ->
                        emit(
                            IndexedValue(
                                index,
                                if (messages.isEmpty()) DiscussionTopicResult.EmptyInput else failure
                            )
                        )
                    }
                    return@flow
                }
                inference.use {
                    promptMessageClusters.forEachIndexed { index, messages ->
                        val result = if (messages.isEmpty()) {
                            DiscussionTopicResult.EmptyInput
                        } else {
                            runCatching {
                                inference.generateResponse(messages.toPrompt()).toTopicResult()
                            }.getOrElse { throwable ->
                                if (throwable is CancellationException) throw throwable
                                DiscussionTopicResult.InferenceFailed(
                                    throwable.message ?: throwable::class.java.simpleName
                                )
                            }
                        }
                        emit(IndexedValue(index, result))
                    }
                }
            }

            is AiInferenceTarget.WireLlm -> {
                promptMessageClusters.forEachIndexed { index, messages ->
                    val result = if (messages.isEmpty()) {
                        DiscussionTopicResult.EmptyInput
                    } else {
                        runCatching {
                            wireLlmClient.query(target.serverIp, messages.toPrompt())
                        }.fold(
                            onSuccess = { response ->
                                when (response) {
                                    is WireLlmQueryResult.Success -> response.result.toTopicResult()
                                    is WireLlmQueryResult.Failure -> DiscussionTopicResult.InferenceFailed(response.message)
                                }
                            },
                            onFailure = { throwable ->
                                if (throwable is CancellationException) throw throwable
                                DiscussionTopicResult.InferenceFailed(
                                    throwable.message ?: throwable::class.java.simpleName
                                )
                            }
                        )
                    }
                    emit(IndexedValue(index, result))
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun List<DiscussionTopicMessage>.toPrompt(): String {
        val transcript = joinToString(separator = "\n") { message ->
            "${message.senderName}: ${message.text.replace('\n', ' ')}"
        }
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

    private companion object {
        const val LITERT_LM_EXTENSION = ".litertlm"
        const val MAX_PROMPT_MESSAGES = 3
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
