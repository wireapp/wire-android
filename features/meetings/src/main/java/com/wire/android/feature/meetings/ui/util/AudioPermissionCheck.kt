/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.feature.meetings.ui.util

import androidx.compose.runtime.Composable
import com.wire.android.feature.meetings.R
import com.wire.android.ui.common.R as commonR
import com.wire.android.ui.common.VisibilityState
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.util.permission.PermissionsDeniedRequestDialog
import com.wire.android.util.permission.RequestLauncher
import com.wire.android.util.permission.rememberRecordAudioPermissionFlow

@Composable
fun audioPermissionCheckFlow(onPermissionGranted: () -> Unit): RequestLauncher {
    val audioPermissionPermanentlyDeniedDialogState = rememberVisibilityState<Unit>()
    VisibilityState(audioPermissionPermanentlyDeniedDialogState) {
        PermissionsDeniedRequestDialog(
            title = commonR.string.app_permission_dialog_title,
            body = R.string.meeting_audio_permission_dialog_description,
            onDismiss = audioPermissionPermanentlyDeniedDialogState::dismiss
        )
    }
    return rememberRecordAudioPermissionFlow(
        onPermissionGranted = onPermissionGranted,
        onPermissionDenied = { },
        onPermissionPermanentlyDenied = {
            audioPermissionPermanentlyDeniedDialogState.show(Unit)
        }
    )
}
