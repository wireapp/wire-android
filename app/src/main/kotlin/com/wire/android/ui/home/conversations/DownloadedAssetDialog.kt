package com.wire.android.ui.home.conversations

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.util.copyDataToTempAssetFile
import com.wire.android.util.getMimeType
import com.wire.android.util.getTempWritableImageUri

@Composable
fun DownloadedAssetDialog(conversationViewModel: ConversationViewModel, onNoActivityFound: () -> Unit) {
    val dialogState = conversationViewModel.conversationViewState.downloadedAssetDialogState
    val context = LocalContext.current
    if (dialogState is DownloadedAssetDialogVisibilityState.Displayed) {
        val assetData = dialogState.assetData
        WireDialog(
            title = dialogState.assetName ?: stringResource(R.string.asset_download_dialog_default_title),
            text = stringResource(R.string.asset_download_dialog_text),
            buttonsHorizontalAlignment = false,
            onDismiss = { conversationViewModel.hideOnAssetDownloadedDialog() },
            optionButton2Properties = WireDialogButtonProperties(
                text = stringResource(R.string.asset_download_dialog_open_text),
                type = WireDialogButtonType.Primary,
                onClick = { openFile(dialogState.assetName, assetData, context, onNoActivityFound) }
            ),
            optionButton1Properties = WireDialogButtonProperties(
                text = stringResource(R.string.asset_download_dialog_save_text),
                type = WireDialogButtonType.Primary,
                onClick = {}
            ),
            dismissButtonProperties = WireDialogButtonProperties(
                text = stringResource(R.string.label_cancel),
                onClick = { conversationViewModel.hideOnAssetDownloadedDialog() }
            ),
        )
    }
}

private fun openFile(assetName: String?, assetData: ByteArray, context: Context, onError: () -> Unit) {
    val fileName = "$assetName"
    val assetUri = copyDataToTempAssetFile(context, fileName, assetData)

    // Set intent and launch
    val intent = Intent()
    intent.action = Intent.ACTION_VIEW
    intent.setDataAndType(assetUri, assetUri.getMimeType(context))

    try {
        context.startActivity(intent)
    } catch (noActivityFoundException: ActivityNotFoundException) {
        appLogger.e("Couldn't find a proper app to process the asset")
        onError()
    }
}
