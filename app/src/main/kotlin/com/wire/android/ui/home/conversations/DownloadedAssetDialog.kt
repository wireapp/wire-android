package com.wire.android.ui.home.conversations

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.util.permission.WriteToExternalStorageRequestFlow

@Composable
fun DownloadedAssetDialog(
    conversationViewState: ConversationViewState,
    onFileSaved: (String?) -> Unit,
    hideOnAssetDownloadedDialog: () -> Unit,
    onOpenFileError: () -> Unit
) {
    val dialogState = conversationViewState.downloadedAssetDialogState
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (dialogState is DownloadedAssetDialogVisibilityState.Displayed) {
        val assetName = dialogState.assetName
        val assetData = dialogState.assetData
        val fileManager = FileManagerImpl(context = context, scope = scope) { name ->
            onFileSaved(name)
            hideOnAssetDownloadedDialog()
        }

        val requestWriteToExternalStorageRequestFlow = WriteToExternalStorageRequestFlow(
            context = context,
            onPermissionGranted = { fileManager.saveToExternalStorage(assetName, assetData) },
            writeToExternalStoragePermissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    fileManager.saveToExternalStorage(assetName, assetData)
                } else {
                    // TODO: Implement denied permission rationale
                }
            })

        WireDialog(
            title = assetName ?: stringResource(R.string.asset_download_dialog_default_title),
            text = stringResource(R.string.asset_download_dialog_text),
            buttonsHorizontalAlignment = false,
            onDismiss = { hideOnAssetDownloadedDialog() },
            optionButton2Properties = WireDialogButtonProperties(
                text = stringResource(R.string.asset_download_dialog_open_text),
                type = WireDialogButtonType.Primary,
                onClick = {
                    fileManager.openWithExternalApp(assetName, assetData) { onOpenFileError() }
                    hideOnAssetDownloadedDialog()
                }
            ),
            optionButton1Properties = WireDialogButtonProperties(
                text = stringResource(R.string.asset_download_dialog_save_text),
                type = WireDialogButtonType.Primary,
                onClick = { requestWriteToExternalStorageRequestFlow.launch() }
            ),
            dismissButtonProperties = WireDialogButtonProperties(
                text = stringResource(R.string.label_cancel),
                onClick = { hideOnAssetDownloadedDialog() }
            ),
        )
    }
}
