package com.wire.android.ui.home.settings.backup.dialog.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType

@Composable
fun FailureDialog(title: String, message: String) {
    WireDialog(
        title = title,
        text = message,
        onDismiss = { },
        optionButton1Properties = WireDialogButtonProperties(
            onClick = { },
            text = stringResource(id = R.string.label_ok),
            type = WireDialogButtonType.Primary,
        )
    )
}
