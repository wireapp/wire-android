/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.feature.cells.ui.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.feature.cells.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.wireDialogPropertiesBuilder
import com.wire.android.ui.theme.WireTheme

@Composable
fun IncompatibleFileNameDialog(
    onReplaceAutomatically: () -> Unit,
    onDismiss: () -> Unit,
) {
    WireDialog(
        title = stringResource(R.string.incompatible_file_name_dialog_title),
        text = stringResource(R.string.incompatible_file_name_dialog_description),
        onDismiss = onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onReplaceAutomatically,
            text = stringResource(R.string.incompatible_file_name_replace_automatically),
            type = WireDialogButtonType.Primary,
        ),
        dismissButtonProperties = WireDialogButtonProperties(
            text = stringResource(R.string.incompatible_file_name_cancel_upload),
            onClick = onDismiss,
        ),
        properties = wireDialogPropertiesBuilder(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    )
}

@MultipleThemePreviews
@Composable
private fun PreviewIncompatibleFileNameDialog() {
    WireTheme {
        IncompatibleFileNameDialog(
            onReplaceAutomatically = {},
            onDismiss = {},
        )
    }
}

