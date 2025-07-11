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

package com.wire.android.ui.common.dialogs.calling

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType

@Composable
fun OngoingActiveCallDialog(onInitiateCallAnyway: () -> Unit, onDialogDismiss: () -> Unit) {
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
            onClick = onInitiateCallAnyway,
            text = stringResource(id = R.string.calling_ongoing_call_start_anyway),
            type = WireDialogButtonType.Primary
        )
    )
}
