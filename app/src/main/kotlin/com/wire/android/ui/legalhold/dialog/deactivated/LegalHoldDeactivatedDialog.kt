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
package com.wire.android.ui.legalhold.dialog.deactivated

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.legalhold.dialog.common.LearnMoreAboutLegalHoldButton
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun LegalHoldDeactivatedDialog(dialogDismissed: () -> Unit) {
    WireDialog(
        title = stringResource(id = R.string.legal_hold_deactivated_dialog_title),
        text = stringResource(id = R.string.legal_hold_deactivated_dialog_description),
        onDismiss = dialogDismissed,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = dialogDismissed,
            text = stringResource(id = R.string.label_ok),
            type = WireDialogButtonType.Primary,
        )
    ) {
        LearnMoreAboutLegalHoldButton(modifier = Modifier.padding(bottom = MaterialTheme.wireDimensions.dialogTextsSpacing))
    }
}

@Composable
@PreviewMultipleThemes
fun PreviewLegalHoldDeactivatedDialog() {
    WireTheme {
        LegalHoldDeactivatedDialog {}
    }
}
