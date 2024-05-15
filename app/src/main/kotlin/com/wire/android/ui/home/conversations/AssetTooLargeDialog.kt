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
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.kalium.logic.data.asset.AttachmentType

@Composable
fun AssetTooLargeDialog(dialogState: AssetTooLargeDialogState, hideDialog: () -> Unit) {
    when (dialogState) {
        is AssetTooLargeDialogState.SingleVisible -> {
            WireDialog(
                title = getTitle(dialogState),
                text = getLabel(dialogState),
                buttonsHorizontalAlignment = false,
                onDismiss = hideDialog,
                optionButton1Properties = WireDialogButtonProperties(
                    text = stringResource(R.string.label_ok),
                    type = WireDialogButtonType.Primary,
                    onClick = hideDialog
                )
            )
        }

        is AssetTooLargeDialogState.MultipleVisible -> {
            WireDialog(
                title = getTitle(dialogState),
                text = getLabel(dialogState),
                buttonsHorizontalAlignment = false,
                onDismiss = hideDialog,
                optionButton1Properties = WireDialogButtonProperties(
                    text = stringResource(R.string.label_ok),
                    type = WireDialogButtonType.Primary,
                    onClick = hideDialog
                )
            )
        }

        AssetTooLargeDialogState.Hidden -> {}
    }
}

@Composable
private fun getTitle(dialogState: AssetTooLargeDialogState) = when (dialogState) {
    AssetTooLargeDialogState.Hidden -> ""
    is AssetTooLargeDialogState.MultipleVisible -> stringResource(id = R.string.title_assets_could_not_be_sent)
    is AssetTooLargeDialogState.SingleVisible -> when (dialogState.assetType) {
        AttachmentType.IMAGE -> stringResource(R.string.title_image_could_not_be_sent)
        AttachmentType.VIDEO -> stringResource(R.string.title_video_could_not_be_sent)
        AttachmentType.AUDIO, // TODO
        AttachmentType.GENERIC_FILE -> stringResource(R.string.title_file_could_not_be_sent)
    }
}

@Composable
private fun getLabel(dialogState: AssetTooLargeDialogState) = when (dialogState) {
    AssetTooLargeDialogState.Hidden -> ""
    is AssetTooLargeDialogState.MultipleVisible -> stringResource(id = R.string.label_shared_asset_too_large, dialogState.maxLimitInMB)
    is AssetTooLargeDialogState.SingleVisible -> when (dialogState.assetType) {
        AttachmentType.IMAGE -> stringResource(R.string.label_shared_image_too_large, dialogState.maxLimitInMB)
        AttachmentType.VIDEO -> stringResource(R.string.label_shared_video_too_large, dialogState.maxLimitInMB)
        AttachmentType.AUDIO, // TODO
        AttachmentType.GENERIC_FILE -> stringResource(R.string.label_shared_file_too_large, dialogState.maxLimitInMB)
    }.let {
        if (dialogState.savedToDevice) it + "\n" + stringResource(R.string.label_file_saved_to_device)
        else it
    }
}

@Preview
@Composable
fun PreviewAssetTooLargeDialog() {
    AssetTooLargeDialog(AssetTooLargeDialogState.SingleVisible(AttachmentType.VIDEO, 100, true)) {}
}

@Preview
@Composable
fun PreviewMultipleAssetTooLargeDialog() {
    AssetTooLargeDialog(AssetTooLargeDialogState.MultipleVisible(20)) {}
}
