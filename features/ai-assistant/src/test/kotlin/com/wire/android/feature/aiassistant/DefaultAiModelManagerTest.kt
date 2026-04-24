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
import com.wire.android.feature.aiassistant.model.AiPromptCapability
import com.wire.android.feature.aiassistant.model.AiModelDownloadState
import com.wire.android.feature.aiassistant.model.AiModelStatus
import com.wire.android.feature.aiassistant.storage.FakeAiModelStorage
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
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

    private class Arrangement(tempDir: Path) {
        val descriptor = AiModelDescriptor(
            displayName = "Test model",
            repositoryId = "google/test-model",
            artifactPath = "test-model.litertlm",
            localDirectoryName = "test-model",
            localFileName = "model.litertlm",
            promptCapability = AiPromptCapability.Weak
        )
        val storage = FakeAiModelStorage(tempDir, descriptor)
        private var downloader: AiModelDownloader = AiModelDownloader {
            flow { emit(AiModelDownloadState.AuthRequired()) }
        }

        fun withFinalModelFile() = apply {
            storage.modelFile.toPath().createFile()
        }

        fun withTempModelFile() = apply {
            storage.tempModelFile.toPath().createFile()
        }

        fun withDownloaderFlow(downloadStates: Flow<AiModelDownloadState>) = apply {
            downloader = AiModelDownloader { downloadStates }
        }

        fun arrange() = Result(
            manager = DefaultAiModelManager(listOf(descriptor), storage, downloader),
            storage = storage
        )
    }

    private data class Result(
        val manager: DefaultAiModelManager,
        val storage: FakeAiModelStorage
    )
}
