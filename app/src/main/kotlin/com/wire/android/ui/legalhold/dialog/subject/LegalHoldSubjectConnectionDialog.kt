/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
package com.wire.android.ui.legalhold.dialog.subject

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun LegalHoldSubjectConnectionDialog(
    userName: String,
    dialogDismissed: () -> Unit,
    connectClicked: () -> Unit,
) {
    LegalHoldSubjectBaseDialog(
        name = userName,
        isConversation = false,
        cancelText = stringResource(id = R.string.label_cancel),
        dialogDismissed = dialogDismissed,
        action = stringResource(id = R.string.connection_label_connect) to connectClicked,
    )
}

@Composable
@PreviewMultipleThemes
fun PreviewLegalHoldSubjectConnectionDialog() {
    WireTheme {
        LegalHoldSubjectConnectionDialog("username", {}, {})
    }
}
