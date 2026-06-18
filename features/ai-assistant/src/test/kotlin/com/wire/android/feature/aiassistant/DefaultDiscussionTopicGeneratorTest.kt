/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.feature.aiassistant

import com.wire.android.feature.aiassistant.model.AiInferenceTarget
import com.wire.android.feature.aiassistant.model.AiModelDownloadState
import com.wire.android.feature.aiassistant.model.AiModelSource
import com.wire.android.feature.aiassistant.model.AiModelStatus
import com.wire.android.feature.aiassistant.test.LiteRtLmInference
import com.wire.android.feature.aiassistant.test.LiteRtLmInferenceFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DefaultDiscussionTopicGeneratorTest {
    @Test
    fun givenWireLlmIsSelected_whenGeneratingTopics_thenEveryPromptUsesRemoteClient() = runTest {
        val client = TopicWireLlmClient()
        val generator = DefaultDiscussionTopicGenerator(
            aiModelManager = TopicModelManager(),
            inferenceConfigStore = TopicInferenceConfigStore(),
            inferenceFactory = TopicInferenceFactory(),
            wireLlmClient = client
        )

        val results = generator.generateTopics(
            listOf(
                listOf(DiscussionTopicMessage("Alice", "Planning the release")),
                listOf(DiscussionTopicMessage("Bob", "Reviewing the test plan"))
            )
        ).toList()

        assertEquals(
            listOf(
                IndexedValue(0, DiscussionTopicResult.Success("Remote topic 1")),
                IndexedValue(1, DiscussionTopicResult.Success("Remote topic 2"))
            ),
            results
        )
        assertEquals(2, client.prompts.size)
        assertTrue(client.prompts.all { it.contains("Identify the main discussion topic") })
    }

    @Test
    fun givenMoreThanThreeUsableMessages_whenGeneratingTopic_thenPromptIncludesOnlyFirstThreeUsableMessages() = runTest {
        val client = TopicWireLlmClient()
        val generator = DefaultDiscussionTopicGenerator(
            aiModelManager = TopicModelManager(),
            inferenceConfigStore = TopicInferenceConfigStore(),
            inferenceFactory = TopicInferenceFactory(),
            wireLlmClient = client
        )

        generator.generateTopic(
            listOf(
                DiscussionTopicMessage("Alice", "First topic detail"),
                DiscussionTopicMessage("Ignored Blank", " "),
                DiscussionTopicMessage("Bob", "Second topic detail"),
                DiscussionTopicMessage("Carol", "Third topic detail"),
                DiscussionTopicMessage("Dave", "Fourth topic detail")
            )
        )

        val prompt = client.prompts.single()
        assertTrue(prompt.contains("Alice: First topic detail"))
        assertTrue(prompt.contains("Bob: Second topic detail"))
        assertTrue(prompt.contains("Carol: Third topic detail"))
        assertFalse(prompt.contains("Ignored Blank"))
        assertFalse(prompt.contains("Dave"))
        assertFalse(prompt.contains("Fourth topic detail"))
    }

    @Test
    fun givenOnDeviceModel_whenGeneratingTopics_thenOneInferenceSessionEmitsOrderedResultsAndContinuesAfterFailure() = runTest {
        val factory = RecordingTopicInferenceFactory(
            listOf(
                Result.failure(IllegalStateException("first failed")),
                Result.success("Second topic")
            )
        )
        val generator = DefaultDiscussionTopicGenerator(
            aiModelManager = TopicModelManager(AiInferenceTarget.OnDevice("/tmp/topic.litertlm")),
            inferenceConfigStore = TopicInferenceConfigStore(),
            inferenceFactory = factory
        )

        val results = generator.generateTopics(
            listOf(
                listOf(DiscussionTopicMessage("Alice", "Planning the release")),
                listOf(DiscussionTopicMessage("Bob", "Reviewing the test plan"))
            )
        ).toList()

        assertEquals(1, factory.createCount)
        assertEquals(1, factory.closeCount)
        assertEquals(
            listOf(
                IndexedValue(0, DiscussionTopicResult.InferenceFailed("first failed")),
                IndexedValue(1, DiscussionTopicResult.Success("Second topic"))
            ),
            results
        )
    }
}

private class TopicModelManager(
    private val target: AiInferenceTarget = AiInferenceTarget.WireLlm("192.168.1.20")
) : AiModelManager {
    override val availableModels: List<AiModelSource> = listOf(AiModelSource.WireLlm)
    override val selectedModel: StateFlow<AiModelSource> = MutableStateFlow(AiModelSource.WireLlm)
    override fun selectModel(source: AiModelSource) = Unit
    override fun observeModelStatus(): Flow<AiModelStatus> =
        flowOf(AiModelStatus.Ready(target))
    override fun downloadModel(): Flow<AiModelDownloadState> = flowOf()
}

private class TopicInferenceConfigStore : AiInferenceConfigStore {
    override fun observeConfig(): Flow<AiInferenceConfig> = flowOf(AiInferenceConfig.DEFAULT)
    override suspend fun setConfig(config: AiInferenceConfig) = Unit
}

private class TopicInferenceFactory : LiteRtLmInferenceFactory {
    override fun create(
        modelPath: String,
        config: AiInferenceConfig,
        initialExchanges: List<Pair<String, String>>
    ): LiteRtLmInference = error("On-device inference must not be used")
}

private class RecordingTopicInferenceFactory(
    private val responses: List<Result<String>>
) : LiteRtLmInferenceFactory {
    var createCount = 0
    var closeCount = 0

    override fun create(
        modelPath: String,
        config: AiInferenceConfig,
        initialExchanges: List<Pair<String, String>>
    ): LiteRtLmInference {
        createCount++
        var responseIndex = 0
        return object : LiteRtLmInference {
            override fun generateResponse(userMessage: String): String =
                responses[responseIndex++].getOrThrow()

            override fun close() {
                closeCount++
            }
        }
    }
}

private class TopicWireLlmClient : WireLlmClient {
    val prompts = mutableListOf<String>()
    override suspend fun query(serverIp: String, prompt: String): WireLlmQueryResult {
        prompts += prompt
        return WireLlmQueryResult.Success("Remote topic ${prompts.size}")
    }
}
