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
import com.wire.android.feature.aiassistant.model.AiInferenceTarget
import com.wire.android.feature.aiassistant.model.AiModelSource
import com.wire.android.feature.aiassistant.model.AiModelStatus
import com.wire.android.feature.aiassistant.storage.AiModelStorage
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking

class DefaultAiModelManager @Inject constructor(
    @JvmSuppressWildcards private val models: List<AiModelDescriptor>,
    private val storage: AiModelStorage,
    private val downloader: AiModelDownloader,
    private val selectionStore: AiModelSelectionStore,
    private val wireLlmConfigStore: WireLlmConfigStore = EmptyWireLlmConfigStore
) : AiModelManager {

    override val availableModels: List<AiModelSource> =
        models.map(AiModelSource::OnDevice) + AiModelSource.WireLlm

    private val _selectedModel = MutableStateFlow(resolveInitialSelection())
    override val selectedModel: StateFlow<AiModelSource> = _selectedModel.asStateFlow()

    private val activeDownloadStatus = MutableStateFlow<ActiveDownloadState?>(null)

    override fun selectModel(source: AiModelSource) {
        require(source in availableModels) { "Unknown model source: $source" }
        _selectedModel.value = source
        runBlocking {
            selectionStore.setSelectedModelId(source.id)
        }
    }

    override fun observeModelStatus(): Flow<AiModelStatus> =
        combine(_selectedModel, activeDownloadStatus, wireLlmConfigStore.observeServerIp()) { selected, active, serverIp ->
            when (selected) {
                is AiModelSource.OnDevice ->
                    if (active?.descriptor == selected.descriptor) active.status else currentStatus(selected.descriptor)
                AiModelSource.WireLlm ->
                    serverIp?.let(WireLlmServerAddress::normalize)
                        ?.let { AiModelStatus.Ready(AiInferenceTarget.WireLlm(it)) }
                        ?: AiModelStatus.RemoteConfigurationRequired
            }
        }.distinctUntilChanged()

    override fun downloadModel(): Flow<AiModelDownloadState> = flow {
        val descriptor = (_selectedModel.value as? AiModelSource.OnDevice)?.descriptor
        if (descriptor == null) {
            emit(AiModelDownloadState.Failed(com.wire.android.feature.aiassistant.model.FailureReason.InvalidResponse))
            return@flow
        }
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
            AiModelStatus.Ready(AiInferenceTarget.OnDevice(modelFile.absolutePath))
        } else {
            AiModelStatus.NotDownloaded
        }
    }

    private fun AiModelDownloadState.asActiveDownloadStatus(descriptor: AiModelDescriptor): ActiveDownloadState? =
        when (this) {
            AiModelDownloadState.Starting -> ActiveDownloadState(descriptor, AiModelStatus.Downloading(progress = null))
            is AiModelDownloadState.Downloading -> ActiveDownloadState(descriptor, AiModelStatus.Downloading(progress))
            is AiModelDownloadState.AuthRequired,
            is AiModelDownloadState.Failed,
            is AiModelDownloadState.Ready -> null
        }

    private fun resolveInitialSelection(): AiModelSource = runBlocking {
        val selectedModelId = selectionStore.getSelectedModelId()
        availableModels.firstOrNull { it.id == selectedModelId } ?: availableModels.first()
    }

    private data class ActiveDownloadState(
        val descriptor: AiModelDescriptor,
        val status: AiModelStatus.Downloading
    )
}
