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
package com.wire.android.ui.legalhold.dialog.subject

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun LegalHoldSubjectProfileDialog(
    userName: String,
    dialogDismissed: () -> Unit,
) {
    LegalHoldSubjectBaseDialog(
        title = stringResource(id = R.string.legal_hold_subject_dialog_title, userName),
        withDefaultInfo = true,
        cancelText = stringResource(id = R.string.label_close),
        dialogDismissed = dialogDismissed
    )
}

@Composable
fun LegalHoldSubjectProfileSelfDialog(dialogDismissed: () -> Unit) {
    LegalHoldSubjectBaseDialog(
        title = stringResource(id = R.string.legal_hold_subject_self_dialog_title),
        withDefaultInfo = true,
        cancelText = stringResource(id = R.string.label_close),
        dialogDismissed = dialogDismissed
    )
}

@Composable
@PreviewMultipleThemes
fun PreviewLegalHoldSubjectProfileDialog() {
    WireTheme {
        LegalHoldSubjectProfileDialog("username", {})
    }
}

@Composable
@PreviewMultipleThemes
fun PreviewLegalHoldSubjectProfileSelfDialog() {
    WireTheme {
        LegalHoldSubjectProfileSelfDialog {}
    }
}
