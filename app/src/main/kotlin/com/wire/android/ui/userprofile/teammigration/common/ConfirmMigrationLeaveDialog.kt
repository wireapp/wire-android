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
package com.wire.android.ui.userprofile.teammigration.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun ConfirmMigrationLeaveDialog(
    onContinue: () -> Unit,
    onLeave: () -> Unit
) {
    WireDialog(
        title = stringResource(R.string.personal_to_team_migration_confirm_leave_dialog_title),
        text = stringResource(R.string.personal_to_team_migration_confirm_leave_dialog_description),
        onDismiss = { },
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onContinue,
            text = stringResource(R.string.personal_to_team_migration_confirm_leave_dialog_continue_button),
            type = WireDialogButtonType.Primary
        ),
        optionButton2Properties = WireDialogButtonProperties(
            text = stringResource(id = R.string.personal_to_team_migration_confirm_leave_dialog_leave_button),
            onClick = onLeave,
            type = WireDialogButtonType.Secondary
        ),
        buttonsHorizontalAlignment = false
    )
}

@PreviewMultipleThemes
@Composable
private fun ConfirmMigrationLeaveDialogPreview() {
    WireTheme {
        ConfirmMigrationLeaveDialog({}, {})
    }
}
