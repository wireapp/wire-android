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
package com.wire.android.ui.calling.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun MicrophonePermissionDeniedDialog(
    shouldShow: Boolean,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    if (shouldShow) {
        WireDialog(
            title = stringResource(id = R.string.call_permission_dialog_title),
            text = stringResource(id = R.string.call_permission_dialog_description),
            onDismiss = onDismiss,
            dismissButtonProperties = WireDialogButtonProperties(
                onClick = onDismiss,
                text = stringResource(id = R.string.label_not_now),
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
@PreviewMultipleThemes
@Composable
fun PreviewMicrophonePermissionDeniedDialog() {
    MicrophonePermissionDeniedDialog(
        shouldShow = true,
        onDismiss = {},
        onOpenSettings = {}
    )
}
