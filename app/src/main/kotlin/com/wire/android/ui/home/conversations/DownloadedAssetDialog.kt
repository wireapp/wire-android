package com.wire.android.ui.home.conversations

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.util.permission.WriteToExternalStorageRequestFlow

@Composable
fun DownloadedAssetDialog(
    downloadedAssetDialogState: DownloadedAssetDialogVisibilityState,
    onSaveFileToExternalStorage: (String?, ByteArray, String) -> Unit,
    onOpenFileWithExternalApp: (String?, ByteArray) -> Unit,
    hideOnAssetDownloadedDialog: () -> Unit
) {
    val context = LocalContext.current

    if (downloadedAssetDialogState is DownloadedAssetDialogVisibilityState.Displayed) {
        val assetName = downloadedAssetDialogState.assetName
        val assetData = downloadedAssetDialogState.assetData
        val messageId = downloadedAssetDialogState.messageId

        // Flow to get write to external storage permission
        val requestWriteToExternalStorageRequestFlow = WriteToExternalStorageRequestFlow(
            context = context,
            onPermissionGranted = { onSaveFileToExternalStorage(assetName, assetData, messageId) },
            writeToExternalStoragePermissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    onSaveFileToExternalStorage(assetName, assetData, messageId)
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
                onClick = { onOpenFileWithExternalApp(assetName, assetData) }
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
