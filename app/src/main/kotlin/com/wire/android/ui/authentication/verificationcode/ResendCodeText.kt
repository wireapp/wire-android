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

package com.wire.android.ui.authentication.verificationcode

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import com.wire.android.R
<<<<<<< HEAD
import com.wire.android.ui.theme.WireTheme
=======
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
>>>>>>> 37030776a (fix: resend code timer [WPB-18364] (#4145))
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun ResendCodeText(
    onResendCodePressed: () -> Unit,
    clickEnabled: Boolean,
    modifier: Modifier = Modifier,
    timerText: String? = null,
) {
    val enabled = timerText == null && clickEnabled
    val label = stringResource(R.string.create_account_code_resend)
    Text(
<<<<<<< HEAD
        text = stringResource(R.string.create_account_code_resend),
        style = MaterialTheme.wireTypography.body02.copy(textDecoration = TextDecoration.Underline),
=======
        text = timerText?.let { "$label ($it)" } ?: label,
        style = MaterialTheme.wireTypography.body02.copy(
            textDecoration = if (enabled) {
                TextDecoration.Underline
            } else {
                TextDecoration.None
            },
            color = if (enabled) {
                MaterialTheme.wireColorScheme.primary
            } else {
                MaterialTheme.wireColorScheme.onSurface
            }
        ),
>>>>>>> 37030776a (fix: resend code timer [WPB-18364] (#4145))
        textAlign = TextAlign.Center,
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onResendCodePressed
            )
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewResendCodeText() = WireTheme {
    ResendCodeText(onResendCodePressed = {}, clickEnabled = true)
}
