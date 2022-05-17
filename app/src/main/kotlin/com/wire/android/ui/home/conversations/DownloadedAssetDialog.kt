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
    conversationViewState: ConversationViewState,
    onSaveFileToExternalStorage: (String?, ByteArray) -> Unit,
    onOpenFileWithExternalApp: (String?, ByteArray) -> Unit,
    hideOnAssetDownloadedDialog: () -> Unit
) {
    val dialogState = conversationViewState.downloadedAssetDialogState
    val context = LocalContext.current

    if (dialogState is DownloadedAssetDialogVisibilityState.Displayed) {
        val assetName = dialogState.assetName
        val assetData = dialogState.assetData

        // Flow to get write to external storage permission
        val requestWriteToExternalStorageRequestFlow = WriteToExternalStorageRequestFlow(
            context = context,
            onPermissionGranted = { onSaveFileToExternalStorage(assetName, assetData) },
            writeToExternalStoragePermissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    onSaveFileToExternalStorage(assetName, assetData)
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
