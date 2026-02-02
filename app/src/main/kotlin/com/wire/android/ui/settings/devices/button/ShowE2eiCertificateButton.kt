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
package com.wire.android.ui.settings.devices.button

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.ui.common.R as commonR

@Composable
fun ShowE2eiCertificateButton(
    enabled: Boolean,
    isLoading: Boolean,
    onShowCertificateClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    WireSecondaryButton(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = dimensions().spacing8x,
                bottom = dimensions().spacing8x
            ),
        text = stringResource(id = R.string.show_e2ei_certificate_details_button),
        fillMaxWidth = true,
        onClick = onShowCertificateClicked,
        loading = isLoading,
        state = if (!enabled) WireButtonState.Disabled else WireButtonState.Default,
        trailingIcon = {
            Icon(
                painter = painterResource(commonR.drawable.ic_chevron_right),
                contentDescription = null,
            )
        }
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewShowE2eiCertificateButton() {
    ShowE2eiCertificateButton(
        enabled = true,
        isLoading = false,
        onShowCertificateClicked = {}
    )
}
