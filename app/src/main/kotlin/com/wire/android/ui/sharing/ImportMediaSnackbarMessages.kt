package com.wire.android.ui.sharing

import com.wire.android.R
import com.wire.android.model.SnackBarMessage
import com.wire.android.util.ui.UIText

sealed class ImportMediaSnackbarMessages(override val uiText: UIText) : SnackBarMessage {
    object MaxImageSize : ImportMediaSnackbarMessages(UIText.StringResource(R.string.error_conversation_max_image_size_limit))
    object MaxAmountOfAssetsReached :
        ImportMediaSnackbarMessages(UIText.StringResource(R.string.error_limit_number_assets_imported_exceeded))
    class MaxAssetSizeExceeded(assetSizeLimit: Int) :
        ImportMediaSnackbarMessages(UIText.StringResource(R.string.error_conversation_max_asset_size_limit, assetSizeLimit))
}
