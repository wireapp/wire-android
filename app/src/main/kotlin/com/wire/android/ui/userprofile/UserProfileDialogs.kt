package com.wire.android.ui.userprofile

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.model.UserStatus
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.theme.wireTypography
import io.github.esentsov.PackagePrivate

@PackagePrivate
@Composable
fun DialogContent(
    dialogState: DialogState,
    dismiss: () -> Unit = {},
    onStatusChange: (UserStatus) -> Unit = {},
    onNotShowRationaleAgainChange: (Boolean, UserStatus) -> Unit = { _, _ -> }
) {
    if (dialogState is DialogState.StatusInfo) {
        UserStatusChangeDialogContent(dialogState, dismiss, onStatusChange, onNotShowRationaleAgainChange)
    }
}

@Composable
private fun UserStatusChangeDialogContent(
    dialogState: DialogState.StatusInfo,
    dismiss: () -> Unit = {},
    onStatusChange: (UserStatus) -> Unit = {},
    onNotShowRationaleAgainChange: (Boolean, UserStatus) -> Unit = { _, _ -> }
) {
    WireDialog(
        title = stringResource(id = dialogState.title),
        text = stringResource(id = dialogState.text),
        onDismiss = dismiss,
        confirmButtonProperties = WireDialogButtonProperties(
            onClick = { onStatusChange(dialogState.status) },
            text = stringResource(id = R.string.label_ok),
            type = WireDialogButtonType.Primary,
        ),
        dismissButtonProperties = WireDialogButtonProperties(
            onClick = dismiss,
            text = stringResource(id = android.R.string.cancel),
            type = WireDialogButtonType.Secondary,
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = dialogState.isCheckBoxChecked,
                onCheckedChange = { onNotShowRationaleAgainChange(it, dialogState.status) }
            )

            Text(
                text = stringResource(R.string.user_profile_change_status_dialog_checkbox_text),
                style = MaterialTheme.wireTypography.body01
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = false)
@Composable
private fun StatusChangeDialogPreview() {
    DialogContent(DialogState.StatusInfo.StateAvailable())
}

