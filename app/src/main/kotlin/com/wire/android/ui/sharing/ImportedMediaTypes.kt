package com.wire.android.ui.sharing

import androidx.compose.runtime.Composable
import com.wire.android.model.Clickable
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.model.MessageGenericAsset
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.util.fileExtension

@Composable
fun ImportedMediaItemView(item: ImportedMediaAsset, isMultipleItemsImport: Boolean) {
    when (item) {
        is ImportedMediaAsset.GenericAsset -> {
            ImportedGenericAssetView(item, isMultipleItemsImport)
        }

        is ImportedMediaAsset.Image -> {
            ImportedImageView(item)
        }
    }
}

@Composable
fun ImportedImageView(item: ImportedMediaAsset.Image) {
    // Try to reuse the PrivateAsset class
}

@Composable
fun ImportedGenericAssetView(item: ImportedMediaAsset.GenericAsset, isMultipleItemsImport: Boolean) {
    MessageGenericAsset(
        item.name,
        item.name.fileExtension() ?: "",
        item.size,
        Clickable(enabled = false),
        Message.UploadStatus.NOT_UPLOADED,
        Message.DownloadStatus.NOT_DOWNLOADED,
        if (isMultipleItemsImport) dimensions().importedMediaAssetSize else null
    )
}

sealed class ImportedMediaAsset(open val name: String, open val size: Long, open val mimeType: String) {
    class GenericAsset(override val name: String, override val size: Long, override val mimeType: String) :
        ImportedMediaAsset(name, size, mimeType)

    class Image(override val name: String, override val size: Long, override val mimeType: String) :
        ImportedMediaAsset(name, size, mimeType)
}
