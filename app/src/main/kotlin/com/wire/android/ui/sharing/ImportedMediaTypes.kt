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
package com.wire.android.ui.sharing

import androidx.compose.runtime.Composable
import com.wire.android.model.Clickable
import com.wire.android.model.ImageAsset
import com.wire.android.ui.home.conversations.model.MessageGenericAsset
import com.wire.android.ui.home.conversations.model.MessageImage
import com.wire.android.ui.home.conversations.model.messagetypes.image.ImageMessageParams
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.util.fileExtension
import com.wire.kalium.logic.util.splitFileExtension
import okio.Path

@Composable
fun ImportedMediaItemView(item: ImportedMediaAsset, isMultipleImport: Boolean) {
    when (item) {
        is ImportedMediaAsset.GenericAsset -> ImportedGenericAssetView(item, isMultipleImport)
        is ImportedMediaAsset.Image -> ImportedImageView(item, isMultipleImport)
    }
}

@Composable
fun ImportedImageView(item: ImportedMediaAsset.Image, isMultipleImport: Boolean) {
    MessageImage(
        asset = item.localImageAsset,
        imgParams = ImageMessageParams(item.width, item.height),
        transferStatus = AssetTransferStatus.NOT_PROCESSED,
        onImageClick = Clickable(enabled = false),
        shouldFillMaxWidth = !isMultipleImport,
        isImportedMediaAsset = true
    )
}

@Composable
fun ImportedGenericAssetView(item: ImportedMediaAsset.GenericAsset, isMultipleImport: Boolean) {
    MessageGenericAsset(
        assetName = item.name.splitFileExtension().first,
        assetExtension = item.name.fileExtension() ?: "",
        assetSizeInBytes = item.size,
        onAssetClick = Clickable(enabled = false),
        assetTransferStatus = AssetTransferStatus.NOT_PROCESSED,
        shouldFillMaxWidth = !isMultipleImport,
        isImportedMediaAsset = true
    )
}

sealed class ImportedMediaAsset(
    open val name: String,
    open val size: Long,
    open val mimeType: String,
    open val dataPath: Path,
    open val key: String
) {
    class GenericAsset(
        override val name: String,
        override val size: Long,
        override val mimeType: String,
        override val dataPath: Path,
        override val key: String
    ) : ImportedMediaAsset(name, size, mimeType, dataPath, key)

    class Image(
        val width: Int,
        val height: Int,
        override val name: String,
        override val size: Long,
        override val mimeType: String,
        override val dataPath: Path,
        override val key: String,
        val wireSessionImageLoader: WireSessionImageLoader
    ) : ImportedMediaAsset(name, size, mimeType, dataPath, key) {
        val localImageAsset = ImageAsset.LocalImageAsset(wireSessionImageLoader, dataPath, key)
    }
}
