package com.wire.android.ui.userprofile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.model.UserStatus
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.WireLabelledCheckbox
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

@OptIn(ExperimentalMaterial3Api::class)
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
        WireLabelledCheckbox(
            label = stringResource(R.string.user_profile_change_status_dialog_checkbox_text),
            checked = data.isCheckBoxChecked,
            onCheckClicked = onNotShowRationaleAgainChange,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = false)
@Composable
private fun ChangeStatusDialogPreview() {
    ChangeStatusDialogContent(StatusDialogData.StateAvailable())
}
