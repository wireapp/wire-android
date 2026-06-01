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

import com.wire.android.feature.aiassistant.download.AiModelDownloader
import com.wire.android.feature.aiassistant.model.AiModelDescriptor
import com.wire.android.feature.aiassistant.model.AiModelDownloadState
import com.wire.android.feature.aiassistant.model.AiModelStatus
import com.wire.android.feature.aiassistant.model.DefaultAiEmbeddingModelDescriptor
import com.wire.android.feature.aiassistant.model.FailureReason
import com.wire.android.feature.aiassistant.storage.AiModelStorage
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class DefaultAiEmbeddingModelManager @Inject constructor(
    private val storage: AiModelStorage,
    private val downloader: AiModelDownloader
) : AiEmbeddingModelManager {

    private val modelDescriptor = DefaultAiEmbeddingModelDescriptor.model
    private val requiredArtifacts = DefaultAiEmbeddingModelDescriptor.requiredArtifacts
    private val activeDownloadStatus = MutableStateFlow<AiModelStatus.Downloading?>(null)

    override fun observeModelStatus(): Flow<AiModelStatus> =
        activeDownloadStatus
            .map { active -> active ?: currentStatus() }
            .distinctUntilChanged()

    override fun downloadModel(): Flow<AiModelDownloadState> = flow {
        if (isReady()) {
            emit(AiModelDownloadState.Ready(modelPath()))
            return@flow
        }

        activeDownloadStatus.value = AiModelStatus.Downloading(progress = null)
        emit(AiModelDownloadState.Starting)

        try {
            var terminalState: AiModelDownloadState? = null
            for ((index, descriptor) in requiredArtifacts.withIndex()) {
                downloader.download(descriptor).collect { state ->
                    when (state) {
                        AiModelDownloadState.Starting -> {
                            val progress = index.toFloat() / requiredArtifacts.size.toFloat()
                            activeDownloadStatus.value = AiModelStatus.Downloading(progress)
                            emit(AiModelDownloadState.Downloading(progress))
                        }
                        is AiModelDownloadState.Downloading -> {
                            val progress = state.progress?.aggregateProgress(index)
                            activeDownloadStatus.value = AiModelStatus.Downloading(progress)
                            emit(AiModelDownloadState.Downloading(progress))
                        }
                        is AiModelDownloadState.AuthRequired -> {
                            cleanupBundle()
                            terminalState = state
                        }
                        is AiModelDownloadState.Failed -> {
                            cleanupBundle()
                            terminalState = state
                        }
                        is AiModelDownloadState.Ready -> {
                            // The bundle is ready only after all required artifacts are present.
                        }
                    }
                }
                terminalState?.let { state ->
                    emit(state)
                    return@flow
                }
            }

            if (isReady()) {
                emit(AiModelDownloadState.Ready(modelPath()))
            } else {
                cleanupBundle()
                emit(AiModelDownloadState.Failed(FailureReason.Storage))
            }
        } catch (exception: CancellationException) {
            cleanupBundle()
            throw exception
        } finally {
            activeDownloadStatus.value = null
        }
    }

    private fun Float.aggregateProgress(index: Int): Float =
        (index.toFloat() + this) / requiredArtifacts.size.toFloat()

    private fun currentStatus(): AiModelStatus =
        if (isReady()) {
            AiModelStatus.Ready(modelPath())
        } else {
            AiModelStatus.NotDownloaded
        }

    private fun isReady(): Boolean =
        requiredArtifacts.all { descriptor -> storage.getModelFile(descriptor).exists() }

    private fun modelPath(): String = storage.getModelFile(modelDescriptor).absolutePath

    private fun cleanupBundle() {
        requiredArtifacts.forEach(::deleteModelFile)
    }

    private fun deleteModelFile(descriptor: AiModelDescriptor) {
        storage.deleteModelFile(descriptor)
    }
}
