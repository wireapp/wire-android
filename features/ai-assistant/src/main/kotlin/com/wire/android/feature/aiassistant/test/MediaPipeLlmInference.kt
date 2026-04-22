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

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface MediaPipeLlmInferenceFactory {
    fun create(modelPath: String): MediaPipeLlmInference
}

interface MediaPipeLlmInference : AutoCloseable {
    fun generateResponse(prompt: String): String
}

class DefaultMediaPipeLlmInferenceFactory @Inject constructor(
    @ApplicationContext private val context: Context
) : MediaPipeLlmInferenceFactory {
    override fun create(modelPath: String): MediaPipeLlmInference {
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .build()
        return DefaultMediaPipeLlmInference(LlmInference.createFromOptions(context, options))
    }
}

private class DefaultMediaPipeLlmInference(
    private val llmInference: LlmInference
) : MediaPipeLlmInference {
    override fun generateResponse(prompt: String): String = llmInference.generateResponse(prompt)

    override fun close() {
        llmInference.close()
    }
}
