package com.wire.android.ui.userprofile.self

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType

@Composable
fun MaxAccountReachedDialog(onConfirm: () -> Unit, onDismiss: () -> Unit, @StringRes buttonText: Int) {
    WireDialog(
        title = stringResource(id = R.string.max_account_reached_dialog_title),
        text = stringResource(id = R.string.max_account_reached_dialog_message),
        onDismiss = onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            text = stringResource(buttonText),
            onClick = onConfirm,
            type = WireDialogButtonType.Primary
        )
    )
}

@Preview(widthDp = 400, heightDp = 800)
@Composable
fun PreviewMaxAccountReachedDialogWithOkButton() {
    MaxAccountReachedDialog(onConfirm = { }, onDismiss = { }, buttonText = R.string.label_ok)
}

@Preview(widthDp = 400, heightDp = 800)
@Composable
fun PreviewMaxAccountReachedDialogWithOpenProfileButton() {
    MaxAccountReachedDialog(onConfirm = { }, onDismiss = { }, buttonText = R.string.max_account_reached_dialog_button_open_profile
    )
}
