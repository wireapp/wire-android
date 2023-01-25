package com.wire.android.ui.home.conversations

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.util.permission.rememberWriteStorageRequestFlow
import com.wire.kalium.logic.util.fileExtension
import okio.Path

@Composable
fun DownloadedAssetDialog(
    downloadedAssetDialogState: DownloadedAssetDialogVisibilityState,
    onSaveFileToExternalStorage: (String, Path, Long, String) -> Unit,
    onOpenFileWithExternalApp: (Path, String?) -> Unit,
    hideOnAssetDownloadedDialog: () -> Unit
) {
    if (downloadedAssetDialogState is DownloadedAssetDialogVisibilityState.Displayed) {
        val assetName = downloadedAssetDialogState.assetName
        val assetDataPath = downloadedAssetDialogState.assetDataPath
        val assetSize = downloadedAssetDialogState.assetSize
        val messageId = downloadedAssetDialogState.messageId

        val onSaveFileWriteStorageRequest = rememberWriteStorageRequestFlow(
            onGranted = { onSaveFileToExternalStorage(assetName, assetDataPath, assetSize, messageId) },
            onDenied = { /** TODO: Show a dialog rationale explaining why the permission is needed **/ }
        )

        WireDialog(
            title = assetName,
            text = stringResource(R.string.asset_download_dialog_text),
            buttonsHorizontalAlignment = false,
            onDismiss = { hideOnAssetDownloadedDialog() },
            optionButton2Properties = WireDialogButtonProperties(
                text = stringResource(R.string.asset_download_dialog_open_text),
                type = WireDialogButtonType.Primary,
                onClick = { onOpenFileWithExternalApp(assetDataPath, assetName.fileExtension()) }
            ),
            optionButton1Properties = WireDialogButtonProperties(
                text = stringResource(R.string.asset_download_dialog_save_text),
                type = WireDialogButtonType.Primary,
                onClick = onSaveFileWriteStorageRequest::launch
            ),
            dismissButtonProperties = WireDialogButtonProperties(
                text = stringResource(R.string.label_cancel),
                onClick = { hideOnAssetDownloadedDialog() }
            ),
        )
    }
}
