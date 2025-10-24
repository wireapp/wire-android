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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun GetE2eiCertificateButton(
    enabled: Boolean,
    isLoading: Boolean,
    onGetCertificateClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    WirePrimaryButton(
        text = stringResource(id = R.string.get_e2ei_certificate_button),
        fillMaxWidth = true,
        onClick = onGetCertificateClicked,
        loading = isLoading,
        state = if (!enabled) {
            WireButtonState.Disabled
        } else {
            WireButtonState.Default
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = dimensions().spacing8x,
                bottom = dimensions().spacing8x,
            )
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewGetE2eiCertificateButton() {
    GetE2eiCertificateButton(
        enabled = true,
        isLoading = false,
        onGetCertificateClicked = {}
    )
}
