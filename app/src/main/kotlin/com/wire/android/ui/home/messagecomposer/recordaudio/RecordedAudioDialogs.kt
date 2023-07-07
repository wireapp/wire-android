/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
package com.wire.android.ui.home.messagecomposer.recordaudio

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState

@Composable
fun DiscardRecordedAudioDialog(
    dialogState: RecordAudioDialogState,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit
) {
    if (dialogState is RecordAudioDialogState.Shown) {
        WireDialog(
            title = stringResource(id = R.string.record_audio_discard_dialog_title),
            text = stringResource(id = R.string.record_audio_discard_dialog_text),
            onDismiss = onDismiss,
            dismissButtonProperties = WireDialogButtonProperties(
                onClick = onDismiss,
                text = stringResource(id = R.string.label_cancel),
                state = WireButtonState.Default
            ),
            optionButton1Properties = WireDialogButtonProperties(
                onClick = onDiscard,
                text = stringResource(id = R.string.record_audio_discard_dialog_discard_button),
                type = WireDialogButtonType.Primary,
                state = WireButtonState.Error
            )
        )
    }
}

@Composable
fun MicrophonePermissionsDeniedDialog(
    dialogState: RecordAudioDialogState,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    if (dialogState is RecordAudioDialogState.Shown) {
        WireDialog(
            title = stringResource(id = R.string.record_audio_permission_denied_dialog_title),
            text = stringResource(id = R.string.record_audio_permission_denied_dialog_text),
            onDismiss = onDismiss,
            dismissButtonProperties = WireDialogButtonProperties(
                onClick = onDismiss,
                text = stringResource(id = R.string.label_cancel),
                state = WireButtonState.Default
            ),
            optionButton1Properties = WireDialogButtonProperties(
                onClick = onOpenSettings,
                text = stringResource(id = R.string.record_audio_permission_denied_dialog_settings_button),
                type = WireDialogButtonType.Primary,
                state = WireButtonState.Default
            )
        )
    }
}

@Composable
fun RecordedAudioMaxFileSizeReachedDialog(
    dialogState: RecordAudioDialogState,
    onDismiss: () -> Unit
) {
    if (dialogState is RecordAudioDialogState.MaxFileSizeReached) {
        WireDialog(
            title = stringResource(id = R.string.record_audio_max_file_size_reached_title),
            text = stringResource(
                id = R.string.record_audio_max_file_size_reached_text,
                dialogState.maxSize
            ),
            onDismiss = onDismiss,
            optionButton1Properties = WireDialogButtonProperties(
                onClick = onDismiss,
                text = stringResource(id = R.string.label_ok),
                type = WireDialogButtonType.Primary,
                state = WireButtonState.Default
            )
        )
    }
}
