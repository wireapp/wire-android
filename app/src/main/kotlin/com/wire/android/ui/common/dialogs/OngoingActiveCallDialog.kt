package com.wire.android.ui.common.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType

@Composable
fun OngoingActiveCallDialog(onJoinAnyways: () -> Unit, onDialogDismiss: () -> Unit) {
    WireDialog(
        title = stringResource(id = R.string.calling_ongoing_call_title_alert),
        text = stringResource(id = R.string.calling_ongoing_call_start_message_alert),
        onDismiss = onDialogDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onDialogDismiss,
            text = stringResource(id = R.string.label_cancel),
            type = WireDialogButtonType.Secondary
        ),
        optionButton2Properties = WireDialogButtonProperties(
            onClick = onJoinAnyways,
            text = stringResource(id = R.string.calling_ongoing_call_start_anyway),
            type = WireDialogButtonType.Primary
        )
    )
}
