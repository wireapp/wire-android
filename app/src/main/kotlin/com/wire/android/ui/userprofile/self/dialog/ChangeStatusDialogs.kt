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

package com.wire.android.ui.userprofile.self.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.WireLabelledCheckbox
import com.wire.kalium.logic.data.user.UserAvailabilityStatus

@Composable
fun ChangeStatusDialogContent(
    data: StatusDialogData?,
    dismiss: () -> Unit = {},
    onStatusChange: (UserAvailabilityStatus) -> Unit = {},
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
    onStatusChange: (UserAvailabilityStatus) -> Unit = {},
    onNotShowRationaleAgainChange: (Boolean) -> Unit = {}
) {
    WireDialog(
        title = stringResource(id = data.title),
        text = stringResource(id = data.text),
        onDismiss = dismiss,
        optionButton1Properties = WireDialogButtonProperties(
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

@Preview(showBackground = false)
@Composable
private fun PreviewChangeStatusDialog() {
    ChangeStatusDialogContent(StatusDialogData.StateAvailable())
}
