package com.wire.android.mapper.message.content.asset

import com.wire.android.ui.home.conversations.model.UIMessageContent
import javax.inject.Inject

class RestrictedAssetMapper @Inject constructor() {
    fun toRestrictedAsset(
        mimeType: String,
        assetSize: Long,
        assetName: String
    ): UIMessageContent {
        return UIMessageContent.RestrictedAsset(
            mimeType = mimeType,
            assetSizeInBytes = assetSize,
            assetName = assetName
        )
    }
}
