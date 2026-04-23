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
import com.wire.android.feature.aiassistant.storage.AiModelStorage
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

class DefaultAiModelManager @Inject constructor(
    @JvmSuppressWildcards private val models: List<AiModelDescriptor>,
    private val storage: AiModelStorage,
    private val downloader: AiModelDownloader
) : AiModelManager {

    override val availableModels: List<AiModelDescriptor> = models

    private val _selectedModel = MutableStateFlow(models.first())
    override val selectedModel: StateFlow<AiModelDescriptor> = _selectedModel.asStateFlow()

    private val activeDownloadStatus = MutableStateFlow<ActiveDownloadState?>(null)

    override fun selectModel(descriptor: AiModelDescriptor) {
        require(descriptor in models) { "Unknown descriptor: $descriptor" }
        _selectedModel.value = descriptor
    }

    override fun observeModelStatus(): Flow<AiModelStatus> =
        combine(_selectedModel, activeDownloadStatus) { selected, active ->
            if (active?.descriptor == selected) {
                active.status
            } else {
                currentStatus(selected)
            }
        }.distinctUntilChanged()

    override fun downloadModel(): Flow<AiModelDownloadState> = flow {
        val descriptor = _selectedModel.value
        val modelFile = storage.getModelFile(descriptor)
        if (modelFile.exists()) {
            emit(AiModelDownloadState.Ready(modelFile.absolutePath))
            return@flow
        }

        try {
            downloader.download(descriptor).collect { state ->
                activeDownloadStatus.value = state.asActiveDownloadStatus(descriptor)
                emit(state)
            }
        } finally {
            activeDownloadStatus.value = null
        }
    }

    private fun currentStatus(descriptor: AiModelDescriptor): AiModelStatus {
        val modelFile = storage.getModelFile(descriptor)
        return if (modelFile.exists()) {
            AiModelStatus.Ready(modelFile.absolutePath)
        } else {
            AiModelStatus.NotDownloaded
        }
    }

    private fun AiModelDownloadState.asActiveDownloadStatus(descriptor: AiModelDescriptor): ActiveDownloadState? =
        when (this) {
            AiModelDownloadState.Starting -> ActiveDownloadState(descriptor, AiModelStatus.Downloading(progress = null))
            is AiModelDownloadState.Downloading -> ActiveDownloadState(descriptor, AiModelStatus.Downloading(progress))
            AiModelDownloadState.AuthRequired,
            is AiModelDownloadState.Failed,
            is AiModelDownloadState.Ready -> null
        }

    private data class ActiveDownloadState(
        val descriptor: AiModelDescriptor,
        val status: AiModelStatus.Downloading
    )
}
