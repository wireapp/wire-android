package com.wire.android.ui.sharing

import android.net.Uri
import androidx.compose.runtime.Composable
import com.wire.android.model.Clickable
import com.wire.android.model.ImageAsset
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.model.MessageGenericAsset
import com.wire.android.ui.home.conversations.model.MessageImage
import com.wire.android.ui.home.conversations.model.messagetypes.image.ImageMessageParams
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.util.fileExtension

@Composable
fun ImportedMediaItemView(item: ImportedMediaAsset, isMultipleImport: Boolean, imageLoader: WireSessionImageLoader) {
    when (item) {
        is ImportedMediaAsset.GenericAsset -> ImportedGenericAssetView(item, isMultipleImport)
        is ImportedMediaAsset.Image -> ImportedImageView(item, imageLoader)
    }
}

@Composable
fun ImportedImageView(item: ImportedMediaAsset.Image, imageLoader: WireSessionImageLoader) {
    MessageImage(
        asset = ImageAsset.LocalImageAsset(imageLoader, item.dataUri),
        imgParams = ImageMessageParams(0, 0),
        uploadStatus = Message.UploadStatus.NOT_UPLOADED,
        downloadStatus = Message.DownloadStatus.NOT_DOWNLOADED,
        Clickable(enabled = false),
        isImportedMediaAsset = true
    )
}

@Composable
fun ImportedGenericAssetView(item: ImportedMediaAsset.GenericAsset, isMultipleImport: Boolean) {
    MessageGenericAsset(
        item.name,
        item.name.fileExtension() ?: "",
        item.size,
        Clickable(enabled = false),
        Message.UploadStatus.NOT_UPLOADED,
        Message.DownloadStatus.NOT_DOWNLOADED,
        !isMultipleImport,
        true
    )
}

sealed class ImportedMediaAsset(open val name: String, open val size: Long, open val mimeType: String, open val dataUri: Uri) {
    class GenericAsset(override val name: String, override val size: Long, override val mimeType: String, override val dataUri: Uri) :
        ImportedMediaAsset(name, size, mimeType, dataUri)

    class Image(override val name: String, override val size: Long, override val mimeType: String, override val dataUri: Uri) :
        ImportedMediaAsset(name, size, mimeType, dataUri)
}
