package com.wire.android.ui.common.error

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.util.dialogErrorStrings
import com.wire.kalium.logic.CoreFailure

@Composable
fun CoreFailureErrorDialog(coreFailure: CoreFailure, onDialogDismiss: () -> Unit) {
    val (title, message) = coreFailure.dialogErrorStrings(LocalContext.current.resources)
    WireDialog(
        title = title,
        text = message,
        onDismiss = onDialogDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onDialogDismiss,
            text = stringResource(id = R.string.label_ok),
            type = WireDialogButtonType.Primary,
        )
    )
}
