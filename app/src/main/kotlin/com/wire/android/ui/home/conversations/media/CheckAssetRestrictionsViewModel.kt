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

    var state: RestrictionCheckState by mutableStateOf(RestrictionCheckState.None)
        private set

    fun checkRestrictions(importedMediaList: List<ImportedMediaAsset>) {
        importedMediaList.firstOrNull { it.assetSizeExceeded != null }.let { assetTooLarge ->
            state = when {
                assetTooLarge != null -> RestrictionCheckState.AssetTooLarge(
                    assetTooLargeDialogStateVisible = AssetTooLargeDialogState.Visible(
                        assetType = assetTooLarge.assetBundle.assetType,
                        maxLimitInMB = assetTooLarge.assetSizeExceeded!!,
                        savedToDevice = false,
                        multipleAssets = importedMediaList.size > 1,
                    )
                )

                else -> RestrictionCheckState.Success(assetBundleList = importedMediaList.map { it.assetBundle })
            }
        }
    }

    fun hideDialog() {
        state = RestrictionCheckState.None
    }
}

sealed interface RestrictionCheckState {
    data object None : RestrictionCheckState
    data class Success(val assetBundleList: List<AssetBundle>) : RestrictionCheckState
    data class AssetTooLarge(val assetTooLargeDialogStateVisible: AssetTooLargeDialogState.Visible) : RestrictionCheckState

    val assetTooLargeDialogState: AssetTooLargeDialogState
        get() = when (this) {
                is AssetTooLarge -> assetTooLargeDialogStateVisible
                else -> AssetTooLargeDialogState.Hidden
            }
}
