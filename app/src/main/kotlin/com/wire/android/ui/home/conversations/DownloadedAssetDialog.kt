package com.wire.android.ui.home.conversations

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.util.getMimeType
import com.wire.android.util.getWritableFileAttachment
import java.io.File

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

fun openFile(assetName: String?, assetData: ByteArray, context: Context, onNoActivityFound: () -> Unit) {
    val fileName = "${System.currentTimeMillis()}_$assetName"
    val tempFile = File(context.cacheDir, fileName)
    tempFile.parentFile?.apply { this.mkdirs() }
    tempFile.writeBytes(assetData)
    val uri = getWritableFileAttachment(context, fileName)
    val intent = Intent()
    intent.action = Intent.ACTION_VIEW
    intent.setDataAndType(uri, uri.getMimeType(context))

    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        onNoActivityFound.invoke()
    }
}
