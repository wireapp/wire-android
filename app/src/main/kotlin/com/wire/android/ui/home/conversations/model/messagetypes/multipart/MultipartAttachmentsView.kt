/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.ui.home.conversations.model.messagetypes.multipart

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.decode.Decoder
import coil.request.ImageRequest
import com.wire.android.ui.common.attachmentdraft.model.AttachmentFileType
import com.wire.android.ui.common.multipart.AssetSource
import com.wire.android.ui.common.multipart.MultipartAttachmentUi
import com.wire.android.ui.common.multipart.toUiModel
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.grid.AssetGridPreview
import com.wire.android.ui.home.conversations.model.messagetypes.multipart.standalone.AssetPreview
import com.wire.android.util.FileManager
import com.wire.kalium.cells.domain.usecase.DownloadCellFileUseCase
import com.wire.kalium.cells.domain.usecase.GetPreviewUrlUseCase
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.MessageAttachment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath
import javax.inject.Inject

@Composable
fun MultipartAttachmentsView(
    conversationId: ConversationId,
    attachments: PersistentList<MessageAttachment>,
    modifier: Modifier = Modifier,
    viewModel: MultipartAttachmentsViewModel = hiltViewModel<MultipartAttachmentsViewModel>(key = conversationId.value),
) {
    when {
        attachments.size > 1 ->
            AttachmentsGrid(
                attachments = attachments.map { it.toUiModel() },
                onClick = { viewModel.onClick(it) },
                onLoadPreview = { viewModel.loadAssetPreview(it) },
                modifier = modifier,
            )
        else ->
            attachments.firstOrNull()?.toUiModel()?.let { item ->
                AssetPreview(
                    item,
                    onClick = { viewModel.onClick(item) },
                    onLoadPreview = { viewModel.loadAssetPreview(item) },
                    modifier = modifier,
                )
            }
    }
}

@Composable
private fun AttachmentsGrid(
    attachments: List<MultipartAttachmentUi>,
    onClick: (MultipartAttachmentUi) -> Unit,
    onLoadPreview: (MultipartAttachmentUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        modifier = modifier.heightIn(max = 1000.dp),
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = attachments,
            key = { it.uuid },
        ) { item ->
            AssetGridPreview(
                item,
                onClick = { onClick(item) },
                onLoadPreview = { onLoadPreview(item) },
            )
        }
    }
}

@HiltViewModel
class MultipartAttachmentsViewModel @Inject constructor(
    private val loadPreview: GetPreviewUrlUseCase,
    private val download: DownloadCellFileUseCase,
    private val fileManager: FileManager,
    private val kaliumFileSystem: KaliumFileSystem,
) : ViewModel() {

    private val refreshed = mutableListOf<String>()

    fun onClick(attachment: MultipartAttachmentUi) {
        when {
            attachment.localFileAvailable() -> openLocalFile(attachment)
            attachment.canOpenWithUrl() -> openUrl(attachment)
            else -> downloadAsset(attachment)
        }
    }

    fun loadAssetPreview(attachment: MultipartAttachmentUi) {
        if (refreshed.contains(attachment.uuid).not()) {
            refreshed.add(attachment.uuid)
            when (attachment.source) {
                AssetSource.CELL -> viewModelScope.launch { loadPreview(attachment.uuid) }
                AssetSource.ASSET_STORAGE -> TODO()
            }
        }
    }

    private fun openLocalFile(attachment: MultipartAttachmentUi) {
        fileManager.openWithExternalApp(
            assetDataPath = attachment.localPath?.toPath() ?: error("No local path"),
            assetName = attachment.fileName ?: "",
            mimeType = attachment.mimeType
        ) {
            Log.e("MessageAttachmentsViewModel", "Failed to open: ${attachment.localPath}")
        }
    }

    private fun openUrl(attachment: MultipartAttachmentUi) {
        fileManager.openUrlWithExternalApp(
            url = attachment.previewUrl ?: error("No preview URL"),
            mimeType = attachment.mimeType
        ) {
            Log.e("MessageAttachmentsViewModel", "Failed to open: ${attachment.previewUrl}")
        }
    }

    private fun downloadAsset(attachment: MultipartAttachmentUi) = viewModelScope.launch {

        val path = kaliumFileSystem.providePersistentAssetPath(attachment.fileName ?: error("No asset path"))

        if (kaliumFileSystem.exists(path)) {
            kaliumFileSystem.delete(path)
        }

        download(attachment.uuid, path)
    }
}

private fun MultipartAttachmentUi.localFileAvailable() = localPath != null
private fun MultipartAttachmentUi.canOpenWithUrl() = previewUrl != null && assetType == AttachmentFileType.IMAGE
internal fun MultipartAttachmentUi.previewAvailable() = localPath != null || previewUrl != null
internal fun MultipartAttachmentUi.getPreview() = localPath ?: previewUrl

@Composable
internal fun MultipartAttachmentUi.previewImageModel(decoderFactory: Decoder.Factory? = null): Any? =

    if (previewAvailable()) {

        val builder = ImageRequest.Builder(LocalContext.current)
            .data(getPreview())
            .crossfade(true)

        if (localPath != null && decoderFactory != null) {
            builder.decoderFactory(decoderFactory)
        }

        builder.build()
    } else {
        null
    }
