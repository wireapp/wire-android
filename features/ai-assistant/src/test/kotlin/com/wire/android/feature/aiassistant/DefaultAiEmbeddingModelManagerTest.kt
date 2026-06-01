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

import app.cash.turbine.test
import com.wire.android.feature.aiassistant.download.AiModelDownloader
import com.wire.android.feature.aiassistant.model.AiModelDescriptor
import com.wire.android.feature.aiassistant.model.AiModelDownloadState
import com.wire.android.feature.aiassistant.model.AiModelStatus
import com.wire.android.feature.aiassistant.model.DefaultAiEmbeddingModelDescriptor
import com.wire.android.feature.aiassistant.model.FailureReason
import com.wire.android.feature.aiassistant.storage.AiModelStorage
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class DefaultAiEmbeddingModelManagerTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun givenBothEmbeddingArtifactsExist_whenObservingStatus_thenReadyIsEmitted() = runTest {
        val arrangement = Arrangement(tempDir)
            .withFinalFile(DefaultAiEmbeddingModelDescriptor.model)
            .withFinalFile(DefaultAiEmbeddingModelDescriptor.tokenizer)
            .arrange()

        arrangement.manager.observeModelStatus().test {
            assertEquals(AiModelStatus.Ready(arrangement.storage.modelFile.absolutePath), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun givenOnlyEmbeddingModelExists_whenObservingStatus_thenNotDownloadedIsEmitted() = runTest {
        val arrangement = Arrangement(tempDir)
            .withFinalFile(DefaultAiEmbeddingModelDescriptor.model)
            .arrange()

        arrangement.manager.observeModelStatus().test {
            assertEquals(AiModelStatus.NotDownloaded, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun givenEmbeddingArtifactsAreMissing_whenDownloading_thenModelAndTokenizerAreDownloadedInOrder() = runTest {
        val arrangement = Arrangement(tempDir)
            .arrange()

        val states = arrangement.manager.downloadModel().toList()

        assertEquals(
            listOf(
                DefaultAiEmbeddingModelDescriptor.model,
                DefaultAiEmbeddingModelDescriptor.tokenizer
            ),
            arrangement.downloader.downloadedDescriptors
        )
        assertEquals(AiModelDownloadState.Ready(arrangement.storage.modelFile.absolutePath), states.last())
        assertTrue(arrangement.storage.modelFile.exists())
        assertTrue(arrangement.storage.tokenizerFile.exists())
    }

    @Test
    fun givenTokenizerRequiresAuthorization_whenDownloading_thenAuthRequiredIsEmittedAndPartialModelIsRemoved() = runTest {
        val arrangement = Arrangement(tempDir)
            .withDownloadState(
                DefaultAiEmbeddingModelDescriptor.tokenizer,
                AiModelDownloadState.AuthRequired(AUTH_REQUIRED_MESSAGE)
            )
            .arrange()

        val states = arrangement.manager.downloadModel().toList()

        assertEquals(AiModelDownloadState.AuthRequired(AUTH_REQUIRED_MESSAGE), states.last())
        assertFalse(arrangement.storage.modelFile.exists())
        assertFalse(arrangement.storage.tokenizerFile.exists())
    }

    @Test
    fun givenTokenizerDownloadFails_whenDownloading_thenFailureIsEmittedAndPartialModelIsRemoved() = runTest {
        val arrangement = Arrangement(tempDir)
            .withDownloadState(
                DefaultAiEmbeddingModelDescriptor.tokenizer,
                AiModelDownloadState.Failed(FailureReason.Network)
            )
            .arrange()

        val states = arrangement.manager.downloadModel().toList()

        assertEquals(AiModelDownloadState.Failed(FailureReason.Network), states.last())
        assertFalse(arrangement.storage.modelFile.exists())
        assertFalse(arrangement.storage.tokenizerFile.exists())
    }

    private class Arrangement(tempDir: Path) {
        val storage = FakeBundleAiModelStorage(tempDir)
        val downloader = RecordingAiModelDownloader(storage)

        fun withFinalFile(descriptor: AiModelDescriptor) = apply {
            storage.ensureModelDirectoryExists(descriptor)
            storage.getModelFile(descriptor).toPath().createFile()
        }

        fun withDownloadState(descriptor: AiModelDescriptor, state: AiModelDownloadState) = apply {
            downloader.downloadStates[descriptor] = flowOf(state)
        }

        fun arrange() = Result(
            manager = DefaultAiEmbeddingModelManager(storage, downloader),
            storage = storage,
            downloader = downloader
        )
    }

    private data class Result(
        val manager: DefaultAiEmbeddingModelManager,
        val storage: FakeBundleAiModelStorage,
        val downloader: RecordingAiModelDownloader
    )

    private companion object {
        const val AUTH_REQUIRED_MESSAGE = "Accept the model license"
    }
}

private class FakeBundleAiModelStorage(tempDir: Path) : AiModelStorage {
    private val modelDirectory = tempDir.resolve(DefaultAiEmbeddingModelDescriptor.model.localDirectoryName).toFile()
    val modelFile = File(modelDirectory, DefaultAiEmbeddingModelDescriptor.model.localFileName)
    val tokenizerFile = File(modelDirectory, DefaultAiEmbeddingModelDescriptor.tokenizer.localFileName)

    override fun getModelFile(descriptor: AiModelDescriptor): File =
        File(modelDirectory, descriptor.localFileName)

    override fun getTempModelFile(descriptor: AiModelDescriptor): File =
        File(modelDirectory, "${descriptor.localFileName}.download")

    override fun ensureModelDirectoryExists(descriptor: AiModelDescriptor) {
        modelDirectory.mkdirs()
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
}

private class RecordingAiModelDownloader(
    private val storage: AiModelStorage
) : AiModelDownloader {
    val downloadedDescriptors = mutableListOf<AiModelDescriptor>()
    val downloadStates = mutableMapOf<AiModelDescriptor, Flow<AiModelDownloadState>>()

    override fun download(descriptor: AiModelDescriptor): Flow<AiModelDownloadState> {
        downloadedDescriptors += descriptor
        return downloadStates[descriptor] ?: flowOf(
            AiModelDownloadState.Starting,
            AiModelDownloadState.Downloading(1F),
            readyState(descriptor)
        )
    }

    private fun readyState(descriptor: AiModelDescriptor): AiModelDownloadState {
        storage.ensureModelDirectoryExists(descriptor)
        storage.getModelFile(descriptor).writeText("model")
        return AiModelDownloadState.Ready(storage.getModelFile(descriptor).absolutePath)
    }
}
