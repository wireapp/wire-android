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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.ScopedArgs
import com.wire.android.di.ViewModelScopedPreview
import com.wire.android.di.scopedArgs
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@Serializable
data class AssetLocalPathArgs(
    val conversationId: ConversationId,
    val messageId: String,
) : ScopedArgs {
    override val key: String = "AssetLocalPathArgs:$conversationId:$messageId"
}

@ViewModelScopedPreview
interface AssetLocalPathViewModel {
    val localAssetPath: String? get() = null
    fun resolveIfNeeded(
        transferStatus: AssetTransferStatus,
        initialAssetDataPath: String?,
        downloadIfNeeded: Boolean = false
    ) {}
}

@HiltViewModel
internal class AssetLocalPathViewModelImpl @Inject constructor(
    private val getMessageAsset: GetMessageAssetUseCase,
    private val dispatchers: DispatcherProvider,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), AssetLocalPathViewModel {
    private val args: AssetLocalPathArgs = savedStateHandle.scopedArgs()

    override var localAssetPath: String? by mutableStateOf(cachedLocalAssetPaths[args.key])
        private set

    private var resolvingJob: Job? = null

    override fun resolveIfNeeded(
        transferStatus: AssetTransferStatus,
        initialAssetDataPath: String?,
        downloadIfNeeded: Boolean
    ) {
        if (initialAssetDataPath != null && localAssetPath != initialAssetDataPath) {
            cachedLocalAssetPaths[args.key] = initialAssetDataPath
            localAssetPath = initialAssetDataPath
        }

        val shouldResolve = when {
            downloadIfNeeded ->
                transferStatus == AssetTransferStatus.NOT_DOWNLOADED ||
                transferStatus == AssetTransferStatus.SAVED_INTERNALLY
            else -> transferStatus == AssetTransferStatus.SAVED_INTERNALLY
        }

        if (!shouldResolve || localAssetPath != null || resolvingJob?.isActive == true) {
            return
        }

        resolvingJob = viewModelScope.launch(dispatchers.io()) {
            try {
                when (val result = getMessageAsset(args.conversationId, args.messageId).await()) {
                    is MessageAssetResult.Success -> {
                        val resolvedPath = result.decodedAssetPath.toString()
                        withContext(dispatchers.main()) {
                            cachedLocalAssetPaths[args.key] = resolvedPath
                            localAssetPath = resolvedPath
                        }
                    }

                    is MessageAssetResult.Failure -> Unit
                }
            } finally {
                withContext(dispatchers.main()) {
                    if (resolvingJob === this@launch) {
                        resolvingJob = null
                    }
                }
            }
        }
    }

    private companion object {
        val cachedLocalAssetPaths = ConcurrentHashMap<String, String>()
    }
}
