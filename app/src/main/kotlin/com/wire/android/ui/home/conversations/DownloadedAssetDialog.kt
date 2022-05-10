package com.wire.android.ui.home.conversations

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType

@Composable
fun DownloadedAssetDialog(conversationViewModel: ConversationViewModel) {
    val dialogState = conversationViewModel.conversationViewState.downloadedAssetDialogState
    if (dialogState is DownloadedAssetDialogVisibilityState.Displayed) {
        WireDialog(
            title = dialogState.assetName ?: stringResource(R.string.asset_download_dialog_default_title),
            text = stringResource(R.string.asset_download_dialog_text),
            buttonsHorizontalAlignment = false,
            onDismiss = {},
            optionButton2Properties = WireDialogButtonProperties(
                text = stringResource(R.string.asset_download_dialog_open_text),
                type = WireDialogButtonType.Primary,
                onClick = { }
            ),
            optionButton1Properties = WireDialogButtonProperties(
                text = stringResource(R.string.asset_download_dialog_save_text),
                type = WireDialogButtonType.Primary,
                onClick = {}
            ),
            dismissButtonProperties = WireDialogButtonProperties(
                text = stringResource(R.string.label_cancel),
                onClick = { conversationViewModel.showOnAssetDownloadedDialog(false, "") }
            ),
        )
    }
}
