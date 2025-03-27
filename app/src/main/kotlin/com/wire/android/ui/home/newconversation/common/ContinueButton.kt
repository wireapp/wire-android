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
package com.wire.android.ui.home.newconversation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.wire.android.R
import com.wire.android.model.ClickBlockParams
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun ContinueButton(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    elevation: Dp = MaterialTheme.wireDimensions.bottomNavigationShadowElevation
) {
    Surface(
        color = MaterialTheme.wireColorScheme.background,
        shadowElevation = elevation
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .padding(horizontal = dimensions().spacing16x)
                .height(dimensions().groupButtonHeight)
        ) {
            WirePrimaryButton(
                text = stringResource(R.string.label_continue),
                leadingIcon = leadingIcon,
                onClick = onContinue,
                state = WireButtonState.Default,
                clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewContinueWithParticipantsCountButton() {
    ContinueButton(
        onContinue = {},
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewSContinueWithParticipantsCountButtonDisabled() {
    ContinueButton(
        onContinue = {},
    )
}
