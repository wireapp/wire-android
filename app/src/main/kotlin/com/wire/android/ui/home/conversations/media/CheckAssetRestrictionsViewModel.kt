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
package com.wire.android.ui.home.conversations.media

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.wire.android.ui.home.conversations.AssetTooLargeDialogState
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.android.ui.sharing.ImportedMediaAsset
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CheckAssetRestrictionsViewModel @Inject constructor() : ViewModel() {

    var assetTooLargeDialogState: AssetTooLargeDialogState by mutableStateOf(
        AssetTooLargeDialogState.Hidden
    )
        private set

    fun checkRestrictions(importedMediaList: List<ImportedMediaAsset>, onSuccess: (bundleList: List<AssetBundle>) -> Unit) {
        importedMediaList.firstOrNull { it.assetSizeExceeded != null }?.let {
            assetTooLargeDialogState = AssetTooLargeDialogState.Visible(
                assetType = it.assetBundle.assetType,
                maxLimitInMB = it.assetSizeExceeded!!,
                savedToDevice = false,
                multipleAssets = true
            )
        } ?: onSuccess(importedMediaList.map { it.assetBundle })
    }

    fun hideDialog() {
        assetTooLargeDialogState = AssetTooLargeDialogState.Hidden
    }
}
