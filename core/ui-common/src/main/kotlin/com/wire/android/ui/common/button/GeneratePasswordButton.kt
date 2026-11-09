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
package com.wire.android.ui.common.button

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.ui.common.R

@Composable
fun GeneratePasswordButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    WireSecondaryButton(
        modifier = modifier,
        onClick = onClick,
        minSize = DpSize(dimensions().spacing32x, dimensions().spacing16x),
        state = WireButtonState.Default,
        fillMaxWidth = false,
        textStyle = MaterialTheme.wireTypography.button03,
        leadingIcon = {
            Icon(
                modifier = Modifier.padding(end = dimensions().corner4x),
                painter = painterResource(id = R.drawable.ic_shield_holo),
                contentDescription = null,
                tint = colorsScheme().onSecondaryButtonEnabled
            )
        },
        leadingIconAlignment = IconAlignment.Center,
        text = stringResource(id = R.string.generate_password_button_text)
    )
}

@Composable
fun CopyPasswordButton(
    password: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val copiedMessage = stringResource(R.string.password_copied)
    val label = stringResource(R.string.login_password_label)
    WireSecondaryButton(
        modifier = modifier,
        onClick = {
            context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(label, password))
            Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
        },
        minSize = DpSize(dimensions().spacing32x, dimensions().spacing16x),
        state = if (password.isEmpty()) WireButtonState.Disabled else WireButtonState.Default,
        fillMaxWidth = false,
        textStyle = MaterialTheme.wireTypography.button03,
        leadingIcon = {
            Icon(
                modifier = Modifier.padding(end = dimensions().corner4x),
                painter = painterResource(R.drawable.ic_copy),
                contentDescription = null,
            )
        },
        leadingIconAlignment = IconAlignment.Center,
        text = stringResource(R.string.copy_password_button_text),
    )
}

@Preview
@Composable
fun GeneratePasswordButtonPreview() {
    GeneratePasswordButton(onClick = {})
}

@Preview
@Composable
fun CopyPasswordButtonPreview() {
    CopyPasswordButton(password = "password")
}
