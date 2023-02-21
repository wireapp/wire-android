package com.wire.android.ui.sharing

import android.net.Uri
import androidx.compose.runtime.Composable
import com.wire.android.model.Clickable
import com.wire.android.model.ImageAsset
import com.wire.android.ui.home.conversations.model.MessageGenericAsset
import com.wire.android.ui.home.conversations.model.MessageImage
import com.wire.android.ui.home.conversations.model.messagetypes.image.ImageMessageParams
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.util.fileExtension
import com.wire.kalium.logic.util.splitFileExtension
import okio.Path

@Composable
fun ImportedMediaItemView(item: ImportedMediaAsset, isMultipleImport: Boolean, imageLoader: WireSessionImageLoader) {
    when (item) {
        is ImportedMediaAsset.GenericAsset -> ImportedGenericAssetView(item, isMultipleImport)
        is ImportedMediaAsset.Image -> ImportedImageView(item, isMultipleImport, imageLoader)
    }
}

@Composable
fun ImportedImageView(item: ImportedMediaAsset.Image, isMultipleImport: Boolean, imageLoader: WireSessionImageLoader) {
    MessageImage(
        asset = ImageAsset.LocalImageAsset(imageLoader, item.dataUri, item.key),
        imgParams = ImageMessageParams(0, 0),
        uploadStatus = Message.UploadStatus.NOT_UPLOADED,
        downloadStatus = Message.DownloadStatus.NOT_DOWNLOADED,
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
        assetUploadStatus = Message.UploadStatus.NOT_UPLOADED,
        assetDownloadStatus = Message.DownloadStatus.NOT_DOWNLOADED,
        shouldFillMaxWidth = !isMultipleImport,
        isImportedMediaAsset = true
    )
}

sealed class ImportedMediaAsset(
    open val name: String,
    open val size: Long,
    open val mimeType: String,
    open val dataPath: Path,
    open val dataUri: Uri,
    open val key: String
) {
    class GenericAsset(
        override val name: String,
        override val size: Long,
        override val mimeType: String,
        override val dataPath: Path,
        override val dataUri: Uri,
        override val key: String
    ) : ImportedMediaAsset(name, size, mimeType, dataPath, dataUri, key)

    class Image(
        val width: Int,
        val height: Int,
        override val name: String,
        override val size: Long,
        override val mimeType: String,
        override val dataPath: Path,
        override val dataUri: Uri,
        override val key: String
    ) : ImportedMediaAsset(name, size, mimeType, dataPath, dataUri, key)
}
