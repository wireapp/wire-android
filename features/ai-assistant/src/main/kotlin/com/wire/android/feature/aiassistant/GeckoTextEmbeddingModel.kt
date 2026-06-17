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

import com.google.ai.edge.localagents.rag.models.EmbedData
import com.google.ai.edge.localagents.rag.models.EmbeddingRequest
import com.google.ai.edge.localagents.rag.models.GeckoEmbeddingModel
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.wire.android.feature.aiassistant.model.DefaultAiEmbeddingModelDescriptor
import com.wire.android.feature.aiassistant.storage.AiModelStorage
import com.wire.kalium.logic.feature.message.TextEmbeddingModel
import java.io.File
import java.util.Optional
import java.util.concurrent.ExecutionException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GeckoTextEmbeddingModel internal constructor(
    private val storage: AiModelStorage,
    private val embedderFactory: GeckoTextEmbedderFactory = DefaultGeckoTextEmbedderFactory
) : TextEmbeddingModel {

    override val modelId: String = MODEL_ID
    override val dimension: Int = DIMENSION
    override val maxTokens: Int = MAX_TOKENS

    private val embedderMutex = Mutex()
    private var embedder: GeckoTextEmbedder? = null

    override suspend fun embed(text: String): FloatArray =
        getEmbedder().embed(text)

    private suspend fun getEmbedder(): GeckoTextEmbedder =
        embedder ?: embedderMutex.withLock {
            embedder ?: createEmbedder().also { embedder = it }
        }

    private fun createEmbedder(): GeckoTextEmbedder {
        val modelFile = requireExistingArtifact(
            storage.getModelFile(DefaultAiEmbeddingModelDescriptor.model),
            "Gecko embedding model"
        )
        val tokenizerFile = requireExistingArtifact(
            storage.getModelFile(DefaultAiEmbeddingModelDescriptor.tokenizer),
            "Gecko SentencePiece tokenizer"
        )
        return embedderFactory.create(
            modelPath = modelFile.absolutePath,
            tokenizerPath = tokenizerFile.absolutePath,
            useGpu = USE_GPU_FOR_EMBEDDINGS
        )
    }

    private fun requireExistingArtifact(file: File, displayName: String): File {
        if (!file.exists()) {
            throw ModelNotReadyException("$displayName is missing at ${file.absolutePath}")
        }
        return file
    }

    class ModelNotReadyException(message: String) : IllegalStateException(message)

    private companion object {
        const val MODEL_ID = "gecko-110m-en-256-quant-v1"
        const val DIMENSION = 768
        const val MAX_TOKENS = 256
        const val USE_GPU_FOR_EMBEDDINGS = false
    }
}

internal interface GeckoTextEmbedderFactory {
    fun create(
        modelPath: String,
        tokenizerPath: String,
        useGpu: Boolean
    ): GeckoTextEmbedder
}

internal interface GeckoTextEmbedder {
    suspend fun embed(text: String): FloatArray
}

private object DefaultGeckoTextEmbedderFactory : GeckoTextEmbedderFactory {
    override fun create(
        modelPath: String,
        tokenizerPath: String,
        useGpu: Boolean
    ): GeckoTextEmbedder =
        GoogleAiEdgeGeckoTextEmbedder(
            GeckoEmbeddingModel(modelPath, Optional.of(tokenizerPath), useGpu)
        )
}

private class GoogleAiEdgeGeckoTextEmbedder(
    private val model: GeckoEmbeddingModel
) : GeckoTextEmbedder {
    override suspend fun embed(text: String): FloatArray {
        val request = EmbeddingRequest.create(
            listOf(
                EmbedData.create(
                    text,
                    EmbedData.TaskType.RETRIEVAL_DOCUMENT
                )
            )
        )
        return model.getEmbeddings(request)
            .await()
            .toFloatArray()
    }

    private fun ImmutableList<Float>.toFloatArray(): FloatArray =
        FloatArray(size) { index -> get(index) }
}

private suspend fun <T> ListenableFuture<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        addListener(
            {
                try {
                    continuation.resume(get())
                } catch (exception: ExecutionException) {
                    continuation.resumeWithException(exception.cause ?: exception)
                } catch (throwable: Throwable) {
                    continuation.resumeWithException(throwable)
                }
            },
            MoreExecutors.directExecutor()
        )
        continuation.invokeOnCancellation { cancel(true) }
    }
