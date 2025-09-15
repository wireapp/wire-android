/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.ui.home.conversations

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.home.conversations.messages.DownloadedAssetDialogVisibilityState
import com.wire.android.util.permission.rememberWriteStoragePermissionFlow

@Composable
fun DownloadedAssetDialog(
    downloadedAssetDialogState: DownloadedAssetDialogVisibilityState,
    onSaveFileToExternalStorage: (String) -> Unit,
    onOpenFileWithExternalApp: (String) -> Unit,
    hideOnAssetDownloadedDialog: () -> Unit,
    onPermissionPermanentlyDenied: () -> Unit
) {
    if (downloadedAssetDialogState is DownloadedAssetDialogVisibilityState.Displayed) {
        val assetName = downloadedAssetDialogState.assetData.fileName
        val messageId = downloadedAssetDialogState.messageId

        val onSaveFileWriteStorageRequest = rememberWriteStoragePermissionFlow(
            onPermissionGranted = { onSaveFileToExternalStorage(messageId) },
            onPermissionDenied = { /** Nothing to do **/ },
            onPermissionPermanentlyDenied = onPermissionPermanentlyDenied
        )

        WireDialog(
            title = assetName,
            text = stringResource(R.string.asset_download_dialog_text),
            buttonsHorizontalAlignment = false,
            onDismiss = { hideOnAssetDownloadedDialog() },
            optionButton1Properties = WireDialogButtonProperties(
                text = stringResource(R.string.asset_download_dialog_open_text),
                type = WireDialogButtonType.Primary,
                onClick = { onOpenFileWithExternalApp(messageId) }
            ),
            optionButton2Properties = WireDialogButtonProperties(
                text = stringResource(R.string.asset_download_dialog_save_text),
                type = WireDialogButtonType.Primary,
                onClick = onSaveFileWriteStorageRequest::launch
            ),
            dismissButtonProperties = WireDialogButtonProperties(
                text = stringResource(R.string.label_cancel),
                onClick = { hideOnAssetDownloadedDialog() }
            ),
        )
    } else if (downloadedAssetDialogState is DownloadedAssetDialogVisibilityState.AlreadyDeleted) {
        WireDialog(
            title = stringResource(R.string.asset_download_not_available_text),
            text = stringResource(R.string.asset_download_deleted_text),
            buttonsHorizontalAlignment = false,
            onDismiss = { hideOnAssetDownloadedDialog() },
            optionButton1Properties = WireDialogButtonProperties(
                text = stringResource(R.string.label_ok),
                type = WireDialogButtonType.Primary,
                onClick = hideOnAssetDownloadedDialog
            ),
        )
    }
}
