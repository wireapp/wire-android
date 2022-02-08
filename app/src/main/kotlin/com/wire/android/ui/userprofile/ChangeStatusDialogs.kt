package com.wire.android.ui.userprofile

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
fun ChangeStatusDialogContent(
    data: StatusDialogData?,
    dismiss: () -> Unit = {},
    onStatusChange: (UserStatus) -> Unit = {},
    onNotShowRationaleAgainChange: (Boolean) -> Unit = {}
) {
    if (data != null) {
        ChangeStatusDialog(data, dismiss, onStatusChange, onNotShowRationaleAgainChange)
    }
}

@Composable
private fun ChangeStatusDialog(
    data: StatusDialogData,
    dismiss: () -> Unit = {},
    onStatusChange: (UserStatus) -> Unit = {},
    onNotShowRationaleAgainChange: (Boolean) -> Unit = {}
) {
    WireDialog(
        title = stringResource(id = data.title),
        text = stringResource(id = data.text),
        onDismiss = dismiss,
        confirmButtonProperties = WireDialogButtonProperties(
            onClick = { onStatusChange(data.status) },
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
                checked = data.isCheckBoxChecked,
                onCheckedChange = onNotShowRationaleAgainChange
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
private fun ChangeStatusDialogPreview() {
    ChangeStatusDialogContent(StatusDialogData.StateAvailable())
}

