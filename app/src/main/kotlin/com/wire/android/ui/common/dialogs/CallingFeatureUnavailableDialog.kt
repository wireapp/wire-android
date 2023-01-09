package com.wire.android.ui.common.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType

@Composable
fun CallingFeatureUnavailableDialog(onDialogDismiss: () -> Unit) {
    WireDialog(
        title = stringResource(id = R.string.calling_feature_unavailable_title_alert),
        text = stringResource(id = R.string.calling_feature_unavailable_message_alert),
        onDismiss = onDialogDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onDialogDismiss,
            text = stringResource(id = R.string.label_ok),
            type = WireDialogButtonType.Primary
        )
    )
}
