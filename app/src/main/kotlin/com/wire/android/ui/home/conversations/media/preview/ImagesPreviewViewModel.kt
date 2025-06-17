/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.media.preview

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.home.conversations.usecase.HandleUriAssetUseCase
import com.wire.android.ui.navArgs
import com.wire.android.ui.sharing.ImportedMediaAsset
import com.wire.android.util.dispatchers.DispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ImagesPreviewViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val handleUriAsset: HandleUriAssetUseCase,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val navArgs: ImagesPreviewNavArgs = savedStateHandle.navArgs()
    var viewState by mutableStateOf(
        ImagesPreviewState(
            conversationId = navArgs.conversationId,
            conversationName = navArgs.conversationName
        )
    )
        private set

    init {
        handleAssets()
    }

    fun onSelected(index: Int) {
        viewState = viewState.copy(selectedIndex = index)
    }

    fun onRemove(index: Int) {
        viewState = viewState.copy(assetBundleList = viewState.assetBundleList.removeAt(index))
    }

    private fun handleAssets() {
        viewState = viewState.copy(isLoading = true)
        viewModelScope.launch {
            val assets = navArgs.assetUriList.map { handleImportedAsset(it) }
            viewState = viewState.copy(
                assetBundleList = assets.filterNotNull().toPersistentList(),
                isLoading = false
            )
        }
    }

    private suspend fun handleImportedAsset(uri: Uri): ImportedMediaAsset? = withContext(dispatchers.io()) {
        when (val result = handleUriAsset.invoke(uri, saveToDeviceIfInvalid = false)) {
            is HandleUriAssetUseCase.Result.Failure.AssetTooLarge -> ImportedMediaAsset(result.assetBundle, result.maxLimitInMB)

            HandleUriAssetUseCase.Result.Failure.Unknown -> null
            is HandleUriAssetUseCase.Result.Success -> ImportedMediaAsset(result.assetBundle, null)
        }
    }
}
