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
package com.wire.android.feature.aiassistant.model

object DefaultAiEmbeddingModelDescriptor {
    val model = AiModelDescriptor(
        displayName = "EmbeddingGemma 300M",
        repositoryId = REPOSITORY_ID,
        artifactPath = "embeddinggemma-300M_seq512_mixed-precision.tflite",
        localDirectoryName = LOCAL_DIRECTORY_NAME,
        localFileName = "model.tflite"
    )

    val tokenizer = AiModelDescriptor(
        displayName = "EmbeddingGemma 300M tokenizer",
        repositoryId = REPOSITORY_ID,
        artifactPath = "sentencepiece.model",
        localDirectoryName = LOCAL_DIRECTORY_NAME,
        localFileName = "sentencepiece.model"
    )

    val requiredArtifacts: List<AiModelDescriptor> = listOf(model, tokenizer)

    private const val REPOSITORY_ID = "litert-community/embeddinggemma-300m"
    private const val LOCAL_DIRECTORY_NAME = "embeddinggemma-300m"
}
