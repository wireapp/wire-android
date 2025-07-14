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
package com.wire.android.feature.sketch

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.feature.sketch.util.PreviewMultipleThemes
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.wireDialogPropertiesBuilder
import com.wire.android.ui.theme.WireTheme

@Composable
internal fun DrawingDiscardConfirmationDialog(
    onDismissSketch: () -> Unit,
    onHideConfirmationDialog: () -> Unit,
) {
    WireDialog(
        title = stringResource(id = R.string.confirm_changes_title),
        text = stringResource(id = R.string.confirm_changes_text),
        onDismiss = onHideConfirmationDialog,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onDismissSketch,
            text = stringResource(id = R.string.confirm_changes_dismiss),
            type = WireDialogButtonType.Primary,
            state = WireButtonState.Error
        ),
        dismissButtonProperties = WireDialogButtonProperties(
            text = stringResource(id = R.string.confirm_changes_confirm),
            onClick = onHideConfirmationDialog
        ),
        properties = wireDialogPropertiesBuilder(dismissOnBackPress = false, dismissOnClickOutside = false)
    )
}

@PreviewMultipleThemes
@Composable
private fun DrawingDiscardConfirmationDialogPreview() = WireTheme {
    DrawingDiscardConfirmationDialog({}, {})
}
