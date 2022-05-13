package com.wire.android.ui.home.conversations

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.util.copyDataToTempFile
import com.wire.android.util.getMimeType
import com.wire.android.util.permission.WriteToExternalStorageRequestFlow
import com.wire.android.util.saveFileDataToDownloadsFolder
import java.io.File

@Composable
fun DownloadedAssetDialog(conversationViewModel: ConversationViewModel) {
    val dialogState = conversationViewModel.conversationViewState.downloadedAssetDialogState
    val context = LocalContext.current

    if (dialogState is DownloadedAssetDialogVisibilityState.Displayed) {
        val assetData = dialogState.assetData
        val onDownloadAsset = {
            saveFileToDownloadsFolder(dialogState.assetName, assetData, context)
            conversationViewModel.onFileSaved(dialogState.assetName)
            conversationViewModel.hideOnAssetDownloadedDialog()
        }
        val requestWriteToExternalStorageRequestFlow =
            WriteToExternalStorageRequestFlow(context, onDownloadAsset, writeToExternalStoragePermissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    onDownloadAsset()
                } else {
                    // TODO: Implement denied permission rationale
                }
            })

        WireDialog(
            title = dialogState.assetName ?: stringResource(R.string.asset_download_dialog_default_title),
            text = stringResource(R.string.asset_download_dialog_text),
            buttonsHorizontalAlignment = false,
            onDismiss = { conversationViewModel.hideOnAssetDownloadedDialog() },
            optionButton2Properties = WireDialogButtonProperties(
                text = stringResource(R.string.asset_download_dialog_open_text),
                type = WireDialogButtonType.Primary,
                onClick = {
                    openAssetFile(dialogState.assetName, assetData, context) { conversationViewModel.onOpenFileError() }
                    conversationViewModel.hideOnAssetDownloadedDialog()
                }
            ),
            optionButton1Properties = WireDialogButtonProperties(
                text = stringResource(R.string.asset_download_dialog_save_text),
                type = WireDialogButtonType.Primary,
                onClick = { requestWriteToExternalStorageRequestFlow.launch() }
            ),
            dismissButtonProperties = WireDialogButtonProperties(
                text = stringResource(R.string.label_cancel),
                onClick = { conversationViewModel.hideOnAssetDownloadedDialog() }
            ),
        )
    }
}

fun saveFileToDownloadsFolder(assetName: String?, assetData: ByteArray, context: Context) {
    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "${System.currentTimeMillis()}_$assetName")
    file.setWritable(true)
    file.writeBytes(assetData)
    context.saveFileDataToDownloadsFolder(file, assetData.size)
}

private fun openAssetFile(assetName: String?, assetData: ByteArray, context: Context, onError: () -> Unit) {
    val fileName = "$assetName"
    val assetUri = copyDataToTempFile(context, fileName, assetData)

    // Set intent and launch
    val intent = Intent()
    intent.action = Intent.ACTION_VIEW
    // These flags allow the external app to access the temporal uri
    intent.flags = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
    } else {
        Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    intent.setDataAndType(assetUri, assetUri.getMimeType(context))

    try {
        context.startActivity(intent)
    } catch (noActivityFoundException: ActivityNotFoundException) {
        appLogger.e("Couldn't find a proper app to process the asset")
        onError()
    }
}
