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

import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import javax.inject.Inject

interface LiteRtLmInferenceFactory {
    fun create(modelPath: String): LiteRtLmInference
}

interface LiteRtLmInference : AutoCloseable {
    fun generateResponse(prompt: String): String
}

class DefaultLiteRtLmInferenceFactory @Inject constructor() : LiteRtLmInferenceFactory {
    override fun create(modelPath: String): LiteRtLmInference {
        val config = EngineConfig(modelPath = modelPath)
        val engine = Engine(config)
        engine.initialize()
        return DefaultLiteRtLmInference(engine)
    }
}

private class DefaultLiteRtLmInference(
    private val engine: Engine
) : LiteRtLmInference {
    override fun generateResponse(prompt: String): String =
        engine.createConversation().use { conversation ->
            conversation.sendMessage(prompt).contents.contents
                .filterIsInstance<Content.Text>()
                .joinToString("") { it.text }
        }

    override fun close() {
        engine.close()
    }
}
