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
import app.cash.turbine.testIn
import app.cash.turbine.turbineScope
import com.wire.android.feature.aiassistant.download.AiModelDownloader
import com.wire.android.feature.aiassistant.model.AiModelDescriptor
import com.wire.android.feature.aiassistant.model.AiModelDownloadState
import com.wire.android.feature.aiassistant.model.AiPromptCapability
import com.wire.android.feature.aiassistant.model.AiModelStatus
import com.wire.android.feature.aiassistant.storage.FakeAiModelStorage
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class DefaultAiModelManagerTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun givenModelFileExists_whenObservingStatus_thenReadyIsEmitted() = runTest {
        val arrangement = Arrangement(tempDir)
            .withFinalModelFile()
            .arrange()

        arrangement.manager.observeModelStatus().test {
            assertEquals(AiModelStatus.Ready(arrangement.storage.modelFile.absolutePath), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun givenOnlyTempFileExists_whenObservingStatus_thenNotDownloadedIsEmitted() = runTest {
        val arrangement = Arrangement(tempDir)
            .withTempModelFile()
            .arrange()

        arrangement.manager.observeModelStatus().test {
            assertEquals(AiModelStatus.NotDownloaded, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun givenDownloadIsActive_whenObservingStatus_thenDownloadingIsEmitted() = runTest {
        val arrangement = Arrangement(tempDir)
            .withDownloaderFlow(
                flow {
                    emit(AiModelDownloadState.Downloading(0.5F))
                    awaitCancellation()
                }
            )
            .arrange()
        turbineScope {
            val statusEvents = arrangement.manager.observeModelStatus().testIn(backgroundScope)

            assertEquals(AiModelStatus.NotDownloaded, statusEvents.awaitItem())

            arrangement.manager.downloadModel().test {
                assertEquals(AiModelDownloadState.Downloading(0.5F), awaitItem())
                assertEquals(AiModelStatus.Downloading(0.5F), statusEvents.awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            statusEvents.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun givenStoredSelectedModelId_whenManagerIsCreated_thenStoredModelIsSelected() = runTest {
        val secondDescriptor = secondDescriptor()
        val arrangement = Arrangement(tempDir)
            .withModels(listOf(descriptor(), secondDescriptor))
            .withStoredSelectedModelId(secondDescriptor.repositoryId)
            .arrange()

        assertEquals(secondDescriptor, arrangement.manager.selectedModel.value)
    }

    @Test
    fun givenMissingStoredSelectedModelId_whenManagerIsCreated_thenFirstModelIsSelected() = runTest {
        val secondDescriptor = secondDescriptor()
        val arrangement = Arrangement(tempDir)
            .withModels(listOf(descriptor(), secondDescriptor))
            .arrange()

        assertEquals(descriptor(), arrangement.manager.selectedModel.value)
    }

    @Test
    fun givenUnknownStoredSelectedModelId_whenManagerIsCreated_thenFirstModelIsSelected() = runTest {
        val secondDescriptor = secondDescriptor()
        val arrangement = Arrangement(tempDir)
            .withModels(listOf(descriptor(), secondDescriptor))
            .withStoredSelectedModelId("unknown/model")
            .arrange()

        assertEquals(descriptor(), arrangement.manager.selectedModel.value)
    }

    @Test
    fun givenKnownDescriptor_whenSelectingModel_thenSelectionIsPersisted() = runTest {
        val secondDescriptor = secondDescriptor()
        val arrangement = Arrangement(tempDir)
            .withModels(listOf(descriptor(), secondDescriptor))
            .arrange()

        arrangement.manager.selectModel(secondDescriptor)

        assertEquals(secondDescriptor, arrangement.manager.selectedModel.value)
        assertEquals(secondDescriptor.repositoryId, arrangement.selectionStore.selectedModelId)
    }

    @Test
    fun givenUnknownDescriptor_whenSelectingModel_thenSelectionFails() = runTest {
        val arrangement = Arrangement(tempDir).arrange()

        assertThrows(IllegalArgumentException::class.java) {
            arrangement.manager.selectModel(secondDescriptor())
        }
    }

    private class Arrangement(private val tempDir: Path) {
        private var models: List<AiModelDescriptor> = listOf(descriptor())
        private var storage = FakeAiModelStorage(tempDir, models.first())
        val selectionStore = FakeAiModelSelectionStore()
        private var downloader: AiModelDownloader = AiModelDownloader {
            flow { emit(AiModelDownloadState.AuthRequired()) }
        }

        fun withFinalModelFile() = apply {
            storage.modelFile.toPath().createFile()
        }

        fun withTempModelFile() = apply {
            storage.tempModelFile.toPath().createFile()
        }

        fun withModels(models: List<AiModelDescriptor>) = apply {
            this.models = models
            storage = FakeAiModelStorage(tempDir, models.first())
        }

        fun withStoredSelectedModelId(modelId: String) = apply {
            selectionStore.selectedModelId = modelId
        }

        fun withDownloaderFlow(downloadStates: Flow<AiModelDownloadState>) = apply {
            downloader = AiModelDownloader { downloadStates }
        }

        fun arrange() = Result(
            manager = DefaultAiModelManager(models, storage, downloader, selectionStore),
            storage = storage,
            selectionStore = selectionStore
        )
    }

    private data class Result(
        val manager: DefaultAiModelManager,
        val storage: FakeAiModelStorage,
        val selectionStore: FakeAiModelSelectionStore
    )

    private companion object {
        fun descriptor() = AiModelDescriptor(
            displayName = "Test model",
            repositoryId = "google/test-model",
            artifactPath = "test-model.litertlm",
            localDirectoryName = "test-model",
            localFileName = "model.litertlm",
            promptCapability = AiPromptCapability.Weak
        )

        fun secondDescriptor() = AiModelDescriptor(
            displayName = "Second test model",
            repositoryId = "google/test-model-2",
            artifactPath = "test-model-2.litertlm",
            localDirectoryName = "test-model-2",
            localFileName = "model.litertlm",
            promptCapability = AiPromptCapability.Capable
        )
    }
}

private class FakeAiModelSelectionStore : AiModelSelectionStore {
    var selectedModelId: String? = null

    override suspend fun getSelectedModelId(): String? = selectedModelId

    override suspend fun setSelectedModelId(modelId: String) {
        selectedModelId = modelId
    }
}
