/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.feature.cells.ui.publiclink.settings.password

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.util.PreviewMultipleThemes
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.theme.WireTheme

@Composable
internal fun PasswordActionsView(
    onCopyPassword: () -> Unit,
    onResetPassword: () -> Unit,
) {
    ActionButton(
        text = R.string.public_link_set_password_copy_password,
        icon = R.drawable.ic_password_copy,
        onClick = onCopyPassword,
    )
    VerticalSpace.x12()
    ActionButton(
        text = R.string.public_link_set_password_reset_password,
        icon = R.drawable.ic_password_reset,
        onClick = onResetPassword,
    )
}

@Composable
private fun ActionButton(text: Int, icon: Int, onClick: () -> Unit) {
    WireSecondaryButton(
        leadingIcon = {
            Box(
                modifier = Modifier.padding(end = dimensions().spacing8x)
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                )
            }
        },
        text = stringResource(text),
        onClick = onClick,
    )
}

@PreviewMultipleThemes
@Composable
private fun PreviewPasswordActionsView() {
    WireTheme {
        Column {
            PasswordActionsView(
                onCopyPassword = {},
                onResetPassword = {},
            )
        }
    }
}
