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
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.ui.home.conversations.messages.item

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.data.asset.AssetTransferStatus.DOWNLOAD_IN_PROGRESS
import com.wire.kalium.logic.data.asset.AssetTransferStatus.FAILED_UPLOAD
import com.wire.kalium.logic.data.asset.AssetTransferStatus.NOT_DOWNLOADED
import com.wire.kalium.logic.data.asset.AssetTransferStatus.SAVED_INTERNALLY
import com.wire.kalium.logic.data.asset.AssetTransferStatus.UPLOADED
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface ConversationAssetPathsViewModel {
    fun localAssetPath(messageId: String): String? = null
    fun localAssetPath(
        conversationId: ConversationId,
        messageId: String,
        assetStatus: AssetTransferStatus?,
        downloadIfNeeded: Boolean = false,
    ): String? = null
}

object ConversationAssetPathsViewModelPreview : ConversationAssetPathsViewModel

@HiltViewModel
class ConversationAssetPathsViewModelImpl @Inject constructor(
    private val getMessageAsset: GetMessageAssetUseCase,
    private val dispatchers: DispatcherProvider,
) : ViewModel(), ConversationAssetPathsViewModel {

    private val localAssetPaths = mutableStateMapOf<String, String>()
    private val resolvingJobs = mutableMapOf<String, Job>()

    override fun localAssetPath(messageId: String): String? = localAssetPaths[messageId]

    override fun localAssetPath(
        conversationId: ConversationId,
        messageId: String,
        assetStatus: AssetTransferStatus?,
        downloadIfNeeded: Boolean,
    ): String? = localAssetPaths[messageId].also { path ->
        if (path == null && resolvingJobs[messageId]?.isActive != true) {
            resolveIfNeeded(
                conversationId = conversationId,
                messageId = messageId,
                transferStatus = assetStatus ?: NOT_DOWNLOADED,
                downloadIfNeeded = downloadIfNeeded,
            )
        }
    }

    private fun resolveIfNeeded(
        conversationId: ConversationId,
        messageId: String,
        transferStatus: AssetTransferStatus,
        downloadIfNeeded: Boolean
    ) {
        if (transferStatus.shouldResolveAsset(downloadIfNeeded)) {
            resolvingJobs[messageId] = viewModelScope.launch(dispatchers.io()) {
                try {
                    when (val result = getMessageAsset(conversationId, messageId).await()) {
                        is MessageAssetResult.Success -> {
                            val resolvedPath = result.decodedAssetPath.toString()
                            withContext(dispatchers.main()) {
                                localAssetPaths[messageId] = resolvedPath
                            }
                        }

                        is MessageAssetResult.Failure -> Unit
                    }
                } finally {
                    withContext(dispatchers.main()) {
                        if (resolvingJobs[messageId] === this@launch) {
                            resolvingJobs.remove(messageId)
                        }
                    }
                }
            }
        }
    }
}

internal fun AssetTransferStatus.shouldResolveAsset(downloadIfNeeded: Boolean) =
    if (downloadIfNeeded) {
        this in setOf(NOT_DOWNLOADED, DOWNLOAD_IN_PROGRESS, SAVED_INTERNALLY, UPLOADED, FAILED_UPLOAD)
    } else {
        this in setOf(SAVED_INTERNALLY, UPLOADED, FAILED_UPLOAD)
    }
