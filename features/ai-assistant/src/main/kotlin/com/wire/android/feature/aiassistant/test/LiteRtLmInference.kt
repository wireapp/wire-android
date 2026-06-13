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
package com.wire.android.feature.aiassistant.test

import android.os.SystemClock
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.wire.android.feature.aiassistant.AiInferenceBackend
import com.wire.android.feature.aiassistant.AiInferenceConfig
import dev.zacsweers.metro.Inject

interface LiteRtLmInferenceFactory {
    fun create(
        modelPath: String,
        config: AiInferenceConfig,
        initialExchanges: List<Pair<String, String>> = emptyList()
    ): LiteRtLmInference
}

interface LiteRtLmInference : AutoCloseable {
    fun generateResponse(userMessage: String): String
}

class DefaultLiteRtLmInferenceFactory @Inject constructor() : LiteRtLmInferenceFactory {
    override fun create(
        modelPath: String,
        config: AiInferenceConfig,
        initialExchanges: List<Pair<String, String>>
    ): LiteRtLmInference {
        val engineConfig = EngineConfig(
            modelPath = modelPath,
            backend = config.toLiteRtBackend()
        )
        val initStartMs = SystemClock.elapsedRealtime()
        val engine = Engine(engineConfig)
        engine.initialize()
        Log.d(TAG, "LiteRT-LM engine initialized in ${SystemClock.elapsedRealtime() - initStartMs}ms (${config.toLogString()})")
        return DefaultLiteRtLmInference(engine, config, initialExchanges)
    }

    private fun AiInferenceConfig.toLiteRtBackend(): Backend =
        when (backend) {
            AiInferenceBackend.CPU -> Backend.CPU(numOfThreads = cpuThreads)
            AiInferenceBackend.GPU -> Backend.GPU()
        }

    private companion object {
        const val TAG = "AiAssistantInference"
    }
}

private class DefaultLiteRtLmInference(
    private val engine: Engine,
    private val config: AiInferenceConfig,
    private val initialExchanges: List<Pair<String, String>>
) : LiteRtLmInference {
    override fun generateResponse(userMessage: String): String {
        val conversationConfig = ConversationConfig(
            initialMessages = initialExchanges.flatMap { (userMsg, modelMsg) ->
                listOf(Message.user(userMsg), Message.model(modelMsg))
            }
        )
        val startMs = SystemClock.elapsedRealtime()
        return engine.createConversation(conversationConfig).use { conversation ->
            val response = conversation.sendMessage(userMessage).contents.contents
                .filterIsInstance<Content.Text>()
                .joinToString("") { it.text }
            Log.d(TAG, "LiteRT-LM inference completed in ${SystemClock.elapsedRealtime() - startMs}ms (${config.toLogString()})")
            response
        }
    }

    override fun close() {
        engine.close()
    }

    private companion object {
        const val TAG = "AiAssistantInference"
    }
}

private fun AiInferenceConfig.toLogString(): String =
    when (backend) {
        AiInferenceBackend.CPU -> "backend=CPU, cpuThreads=${cpuThreads ?: "auto"}"
        AiInferenceBackend.GPU -> "backend=GPU"
    }
