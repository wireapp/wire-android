package com.wire.android.ui.calling.controlButtons

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.permission.rememberCallingRecordAudioBluetoothRequestFlow

@Composable
fun JoinButton(
    buttonClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = MaterialTheme.wireDimensions.buttonMinSize.height,
    minWidth: Dp = MaterialTheme.wireDimensions.buttonMinSize.width
) {
    val audioPermissionCheck = AudioBluetoothPermissionCheckFlow { buttonClick() }

    WirePrimaryButton(
        onClick = audioPermissionCheck::launch,
        fillMaxWidth = false,
        shape = RoundedCornerShape(size = MaterialTheme.wireDimensions.corner12x),
        text = stringResource(R.string.calling_button_label_join_call),
        textStyle = MaterialTheme.wireTypography.button03,
        state = WireButtonState.Positive,
        minHeight = minHeight,
        minWidth = minWidth,
        modifier = modifier.padding(
            top = dimensions().spacing12x,
            bottom = dimensions().spacing12x,
            end = dimensions().spacing8x
        ),
        contentPadding = PaddingValues(
            horizontal = dimensions().spacing8x,
            vertical = dimensions().spacing4x
        )
    )
}

@Composable
private fun AudioBluetoothPermissionCheckFlow(
    onJoinCall: () -> Unit
) = rememberCallingRecordAudioBluetoothRequestFlow(onAudioBluetoothPermissionGranted = {
    appLogger.d("Join Call Button - Permissions granted")
    onJoinCall()
}) {
    appLogger.d("Join Call Button - Permissions denied")
    // TODO: Add a message that user needs permission to join call?
}

@Preview
@Composable
fun JoinButtonPreview() {
    JoinButton(
        buttonClick = {}
    )
}
