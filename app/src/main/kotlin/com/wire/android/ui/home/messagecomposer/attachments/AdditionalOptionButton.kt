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
package com.wire.android.ui.home.messagecomposer.attachments

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.wire.android.R
import com.wire.android.di.hiltViewModelScoped
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryIconButton
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import kotlinx.coroutines.delay

/**
 * Represents an additional option button with controlled interactivity for message composer.
 *
 * The button's clickability can be controlled via the `isSelected` parameter. This composable also
 * internally handles preventing rapid successive clicks using the `enableAgain` variable. This
 * mechanism is particularly important to ensure that, during keyboard transitions (expanding or
 * collapsing), unintended or unexpected repetitive button clicks do not occur, preventing the
 * keyboard from collapsing in an unexpected manner.
 *
 * @param isSelected Indicates whether the button is selected or not.
 * @param onClick The action to be performed when the button is clicked.
 * @param modifier The optional [Modifier] to be applied to this composable.
 */
@Composable
fun AdditionalOptionButton(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IsFileSharingEnabledViewModel =
        hiltViewModelScoped<IsFileSharingEnabledViewModelImpl, IsFileSharingEnabledViewModel, IsFileSharingEnabledViewModelImpl.Factory, IsFileSharingEnabledArgs>(
            IsFileSharingEnabledArgs
        )
) {
    var enableAgain by remember { mutableStateOf(true) }
    LaunchedEffect(enableAgain, block = {
        if (enableAgain) return@LaunchedEffect
        delay(timeMillis = BUTTON_CLICK_DELAY_MILLIS)
        enableAgain = true
    })

    Box(modifier = modifier) {
        WireSecondaryIconButton(
            onButtonClicked = {
                if (enableAgain) {
                    enableAgain = false
                    onClick()
                }
            },
            iconResource = R.drawable.ic_add,
            contentDescription = R.string.content_description_attachment_item,
            state = if (!viewModel.isFileSharingEnabled()) {
                WireButtonState.Disabled
            } else if (isSelected) WireButtonState.Selected else WireButtonState.Default,
        )
    }
}

private const val BUTTON_CLICK_DELAY_MILLIS = 400L

@PreviewMultipleThemes
@Composable
fun PreviewAdditionalOptionButtonUnSelected() {
    WireTheme {
        AdditionalOptionButton(isSelected = false, onClick = {})
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewAdditionalOptionButtonSelected() {
    WireTheme {
        AdditionalOptionButton(isSelected = true, onClick = {})
    }
}
