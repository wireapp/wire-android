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

package com.wire.android.ui.home.settings.multifactorauthentication

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import com.wire.android.R
import com.wire.android.ui.common.WireLabelledCheckbox
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireTypography

@Composable
internal fun RecoveryCodesBackHandler(canNavigateBack: Boolean, onBack: () -> Unit) {
    BackHandler { if (canNavigateBack) onBack() }
}

@Composable
internal fun MultiFactorAuthenticationRecoveryCodesContent(
    codes: List<String>,
    saved: Boolean,
    onSavedChange: (Boolean) -> Unit,
    onCopy: () -> Unit,
    onDownload: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensions().spacing16x)) {
        Text(
            text = codes.joinToString("\n"),
            style = MaterialTheme.wireTypography.body01,
            fontFamily = FontFamily.Monospace,
        )
        WireSecondaryButton(text = stringResource(R.string.multi_factor_authentication_copy_codes), onClick = onCopy)
        WireSecondaryButton(text = stringResource(R.string.multi_factor_authentication_download_codes), onClick = onDownload)
        WireLabelledCheckbox(
            label = stringResource(R.string.multi_factor_authentication_codes_saved),
            checked = saved,
            onCheckClicked = onSavedChange,
        )
    }
}
