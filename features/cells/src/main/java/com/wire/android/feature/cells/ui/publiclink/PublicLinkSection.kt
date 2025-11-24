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
package com.wire.android.feature.cells.ui.publiclink

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.feature.cells.R
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.typography

@Composable
internal fun PublicLinkSection(
    state: PublicLinkState,
    onShareLink: () -> Unit,
    onCopyLink: () -> Unit,
) {

    val isLoading = state == PublicLinkState.LOADING

    Column {
        Text(
            text = stringResource(R.string.share_link).uppercase(),
            style = typography().title03,
            modifier = Modifier.padding(dimensions().spacing16x)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorsScheme().surface)
                .padding(dimensions().spacing16x)
        ) {
            WireSecondaryButton(
                text = stringResource(R.string.share_link),
                state = if (isLoading) WireButtonState.Disabled else WireButtonState.Default,
                loading = isLoading,
                onClick = onShareLink,
            )
            Spacer(modifier = Modifier.height(dimensions().spacing8x))
            WireSecondaryButton(
                text = stringResource(R.string.copy_link),
                state = if (isLoading) WireButtonState.Disabled else WireButtonState.Default,
                loading = isLoading,
                onClick = onCopyLink
            )
        }
    }
}
