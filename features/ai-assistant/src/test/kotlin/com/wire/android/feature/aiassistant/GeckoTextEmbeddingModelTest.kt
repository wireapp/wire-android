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
import com.wire.android.feature.aiassistant.model.AiModelDescriptor
import com.wire.android.feature.aiassistant.model.DefaultAiEmbeddingModelDescriptor
import com.wire.android.feature.aiassistant.storage.AiModelStorage
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createFile
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class GeckoTextEmbeddingModelTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun givenGeckoTextEmbeddingModel_whenInspectingMetadata_thenGecko256QuantMetadataIsExposed() {
        val model = GeckoTextEmbeddingModel(GeckoFakeBundleAiModelStorage(tempDir))

        assertEquals("gecko-110m-en-256-quant-v1", model.modelId)
        assertEquals(768, model.dimension)
        assertEquals(256, model.maxTokens)
    }

    @Test
    fun givenModelFileIsMissing_whenEmbedding_thenModelNotReadyIsThrown() {
        val storage = GeckoFakeBundleAiModelStorage(tempDir)
            .withFinalFile(DefaultAiEmbeddingModelDescriptor.tokenizer)
        val model = GeckoTextEmbeddingModel(storage, FakeGeckoTextEmbedderFactory())

        val exception = assertThrows(GeckoTextEmbeddingModel.ModelNotReadyException::class.java) {
            runTest { model.embed("hello") }
        }

        assertEquals(
            "Gecko embedding model is missing at ${storage.getModelFile(DefaultAiEmbeddingModelDescriptor.model).absolutePath}",
            exception.message
        )
    }

    @Test
    fun givenTokenizerFileIsMissing_whenEmbedding_thenModelNotReadyIsThrown() {
        val storage = GeckoFakeBundleAiModelStorage(tempDir)
            .withFinalFile(DefaultAiEmbeddingModelDescriptor.model)
        val model = GeckoTextEmbeddingModel(storage, FakeGeckoTextEmbedderFactory())

        val exception = assertThrows(GeckoTextEmbeddingModel.ModelNotReadyException::class.java) {
            runTest { model.embed("hello") }
        }

        assertEquals(
            "Gecko SentencePiece tokenizer is missing at " +
                storage.getModelFile(DefaultAiEmbeddingModelDescriptor.tokenizer).absolutePath,
            exception.message
        )
    }

    @Test
    fun givenBothArtifactsExist_whenEmbeddingDocument_thenFactoryReceivesBundlePathsAndDocumentTaskType() = runTest {
        val storage = GeckoFakeBundleAiModelStorage(tempDir)
            .withFinalFile(DefaultAiEmbeddingModelDescriptor.model)
            .withFinalFile(DefaultAiEmbeddingModelDescriptor.tokenizer)
        val factory = FakeGeckoTextEmbedderFactory(vector = floatArrayOf(1F, 2F, 3F))
        val model = GeckoTextEmbeddingModel(storage, factory)

        val embedding = model.embedDocument("hello")

        assertArrayEquals(floatArrayOf(1F, 2F, 3F), embedding)
        assertEquals(
            listOf(
                FactoryCall(
                    modelPath = storage.getModelFile(DefaultAiEmbeddingModelDescriptor.model).absolutePath,
                    tokenizerPath = storage.getModelFile(DefaultAiEmbeddingModelDescriptor.tokenizer).absolutePath,
                    useGpu = true
                )
            ),
            factory.calls
        )
        assertEquals(listOf(EmbedCall("hello", EmbedData.TaskType.RETRIEVAL_DOCUMENT)), factory.embedder.embedCalls)
    }

    @Test
    fun givenBothArtifactsExist_whenEmbeddingQuery_thenQueryTaskTypeIsUsed() = runTest {
        val storage = GeckoFakeBundleAiModelStorage(tempDir)
            .withFinalFile(DefaultAiEmbeddingModelDescriptor.model)
            .withFinalFile(DefaultAiEmbeddingModelDescriptor.tokenizer)
        val factory = FakeGeckoTextEmbedderFactory(vector = floatArrayOf(1F, 2F, 3F))
        val model = GeckoTextEmbeddingModel(storage, factory)

        val embedding = model.embedQuery("hello")

        assertArrayEquals(floatArrayOf(1F, 2F, 3F), embedding)
        assertEquals(listOf(EmbedCall("hello", EmbedData.TaskType.RETRIEVAL_QUERY)), factory.embedder.embedCalls)
    }

    @Test
    fun givenConcurrentEmbeddingRequests_whenEmbedding_thenEmbedderIsCreatedOnce() = runTest {
        val storage = GeckoFakeBundleAiModelStorage(tempDir)
            .withFinalFile(DefaultAiEmbeddingModelDescriptor.model)
            .withFinalFile(DefaultAiEmbeddingModelDescriptor.tokenizer)
        val factory = FakeGeckoTextEmbedderFactory()
        val model = GeckoTextEmbeddingModel(storage, factory)

        listOf("one", "two")
            .map { text -> async { model.embed(text) } }
            .awaitAll()

        assertEquals(1, factory.calls.size)
        assertEquals(listOf("one", "two"), factory.embedder.embedCalls.map { it.text }.sorted())
    }
}

private class FakeGeckoTextEmbedderFactory(
    private val vector: FloatArray = floatArrayOf(0F)
) : GeckoTextEmbedderFactory {
    val calls = mutableListOf<FactoryCall>()
    val embedder = FakeGeckoTextEmbedder(vector)

    override fun create(
        modelPath: String,
        tokenizerPath: String,
        useGpu: Boolean
    ): GeckoTextEmbedder {
        calls += FactoryCall(modelPath, tokenizerPath, useGpu)
        return embedder
    }
}

private class FakeGeckoTextEmbedder(
    private val vector: FloatArray
) : GeckoTextEmbedder {
    val embedCalls = mutableListOf<EmbedCall>()

    override suspend fun embed(text: String, taskType: EmbedData.TaskType): FloatArray {
        embedCalls += EmbedCall(text, taskType)
        return vector
    }
}

private data class EmbedCall(
    val text: String,
    val taskType: EmbedData.TaskType
)

private data class FactoryCall(
    val modelPath: String,
    val tokenizerPath: String,
    val useGpu: Boolean
)

private class GeckoFakeBundleAiModelStorage(tempDir: Path) : AiModelStorage {
    private val rootDirectory = tempDir.toFile()

    override fun getModelFile(descriptor: AiModelDescriptor): File =
        File(getModelDirectory(descriptor), descriptor.localFileName)

    override fun getTempModelFile(descriptor: AiModelDescriptor): File =
        File(getModelDirectory(descriptor), "${descriptor.localFileName}.download")

    override fun ensureModelDirectoryExists(descriptor: AiModelDescriptor) {
        getModelDirectory(descriptor).mkdirs()
    }

    override fun promoteTempFile(descriptor: AiModelDescriptor) {
        Files.move(
            getTempModelFile(descriptor).toPath(),
            getModelFile(descriptor).toPath(),
            StandardCopyOption.REPLACE_EXISTING
        )
    }

    override fun deleteModelFile(descriptor: AiModelDescriptor) {
        getModelFile(descriptor).delete()
        getTempModelFile(descriptor).delete()
    }

    fun withFinalFile(descriptor: AiModelDescriptor) = apply {
        ensureModelDirectoryExists(descriptor)
        getModelFile(descriptor).toPath().createFile()
    }

    private fun getModelDirectory(descriptor: AiModelDescriptor): File =
        File(rootDirectory, descriptor.localDirectoryName)
}
