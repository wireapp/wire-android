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
fun LegalHoldSubjectBaseDialog(
    title: String,
    withDefaultInfo: Boolean,
    cancelText: String,
    dialogDismissed: () -> Unit,
    modifier: Modifier = Modifier,
    customInfo: String? = null,
    action: Pair<String, () -> Unit>? = null
) {
    val text = listOfNotNull(
        customInfo,
        if (withDefaultInfo) stringResource(id = R.string.legal_hold_subject_dialog_description) else null
    ).joinToString("\n\n")
    WireDialog(
        modifier = modifier,
        title = title,
        text = text,
        onDismiss = dialogDismissed,
        buttonsHorizontalAlignment = false,
        optionButton1Properties = action?.let { (actionText, actionClicked) ->
            WireDialogButtonProperties(
                onClick = actionClicked,
                text = actionText,
                type = WireDialogButtonType.Primary,
            )
        },
        optionButton2Properties = WireDialogButtonProperties(
            onClick = dialogDismissed,
            text = cancelText,
            type = WireDialogButtonType.Secondary,
        ),
    ) {
        LearnMoreAboutLegalHoldButton(
            modifier = Modifier.padding(bottom = MaterialTheme.wireDimensions.dialogTextsSpacing)
        )
    }
}

@Composable
@PreviewMultipleThemes
fun PreviewLegalHoldSubjectBaseDialog() {
    WireTheme {
        LegalHoldSubjectBaseDialog(
            title = "username",
            customInfo = null,
            withDefaultInfo = true,
            cancelText = "cancel",
            dialogDismissed = {},
            action = Pair("send anyway") {}
        )
    }
}
