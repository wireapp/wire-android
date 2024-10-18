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
package com.wire.android.ui.userprofile.teammigration

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BottomLineButtons(
    isContinueButtonEnabled: Boolean,
    modifier: Modifier = Modifier,
    isBackButtonVisible: Boolean = true,
    onBack: () -> Unit = { },
    onContinue: () -> Unit = { }
) {
    Column(
        modifier = modifier
            .padding(
                top = dimensions().spacing16x,
                start = dimensions().spacing16x,
                end = dimensions().spacing16x,
                bottom = if (!WindowInsets.isImeVisible) {
                    dimensions().spacing32x
                } else {
                    dimensions().spacing16x
                }
            )
            .imePadding()
    ) {
        if (isBackButtonVisible) {
            WireSecondaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.personal_to_team_migration_back_button_label),
                onClick = onBack
            )
        }

        WirePrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimensions().spacing6x),
            text = stringResource(R.string.label_continue),
            onClick = onContinue,
            state = if (isContinueButtonEnabled) {
                WireButtonState.Default
            } else {
                WireButtonState.Disabled
            }
        )
    }
}

@MultipleThemePreviews
@Composable
fun BottomLineButtonsPreview() {
    BottomLineButtons(
        isContinueButtonEnabled = true
    )
}
