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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class DefaultAiModelManager @Inject constructor(
    private val descriptor: AiModelDescriptor,
    private val storage: AiModelStorage,
    private val downloader: AiModelDownloader
) : AiModelManager {

    private val activeDownloadStatus = MutableStateFlow<AiModelStatus.Downloading?>(null)

    override fun observeModelStatus(): Flow<AiModelStatus> =
        activeDownloadStatus
            .map { activeStatus -> activeStatus ?: currentStatus() }
            .distinctUntilChanged()

    override fun downloadModel(): Flow<AiModelDownloadState> = flow {
        val modelFile = storage.getModelFile(descriptor)
        if (modelFile.exists()) {
            emit(AiModelDownloadState.Ready(modelFile.absolutePath))
            return@flow
        }

        try {
            downloader.download(descriptor).collect { state ->
                activeDownloadStatus.value = state.asActiveDownloadStatus()
                emit(state)
            }
        } finally {
            activeDownloadStatus.value = null
        }
    }

    private fun currentStatus(): AiModelStatus {
        val modelFile = storage.getModelFile(descriptor)
        return if (modelFile.exists()) {
            AiModelStatus.Ready(modelFile.absolutePath)
        } else {
            AiModelStatus.NotDownloaded
        }
    }

    private fun AiModelDownloadState.asActiveDownloadStatus(): AiModelStatus.Downloading? =
        when (this) {
            AiModelDownloadState.Starting -> AiModelStatus.Downloading(progress = null)
            is AiModelDownloadState.Downloading -> AiModelStatus.Downloading(progress)
            AiModelDownloadState.AuthRequired,
            is AiModelDownloadState.Failed,
            is AiModelDownloadState.Ready -> null
        }
}
