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

package com.wire.android.ui.calling.controlbuttons

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.permission.PermissionDenialType
import com.wire.android.util.permission.rememberCallingRecordAudioRequestFlow

@Composable
fun JoinButton(
    buttonClick: () -> Unit,
    onPermissionPermanentlyDenied: (type: PermissionDenialType) -> Unit,
    minSize: DpSize = MaterialTheme.wireDimensions.buttonMediumMinSize,
    minClickableSize: DpSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
    horizontalPadding: Dp = MaterialTheme.wireDimensions.spacing8x,
) {
    val audioPermissionCheck = AudioPermissionCheckFlow(
        onJoinCall = buttonClick,
        onPermanentPermissionDecline = {
            onPermissionPermanentlyDenied(
                PermissionDenialType.CallingMicrophone
            )
        }
    )

    WirePrimaryButton(
        onClick = audioPermissionCheck::launch,
        fillMaxWidth = false,
        shape = RoundedCornerShape(size = MaterialTheme.wireDimensions.corner12x),
        text = stringResource(R.string.calling_button_label_join_call),
        textStyle = MaterialTheme.wireTypography.button03,
        state = WireButtonState.Positive,
        minSize = minSize,
        minClickableSize = minClickableSize,
        modifier = Modifier.padding(
            horizontal = horizontalPadding
        ),
        contentPadding = PaddingValues(
            horizontal = dimensions().spacing8x,
            vertical = dimensions().spacing4x
        )
    )
}

@Composable
private fun AudioPermissionCheckFlow(
    onJoinCall: () -> Unit,
    onPermanentPermissionDecline: () -> Unit
) = rememberCallingRecordAudioRequestFlow(
    onAudioPermissionGranted = {
        appLogger.d("IncomingCall - Audio permission granted")
        onJoinCall()
    },
    onAudioPermissionDenied = { },
    onAudioPermissionPermanentlyDenied = onPermanentPermissionDecline
)

@Preview
@Composable
fun PreviewJoinButton() {
    JoinButton(
        buttonClick = {},
        onPermissionPermanentlyDenied = {}
    )
}
