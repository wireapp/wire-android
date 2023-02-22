/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.home.newconversation.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.wire.android.R
import com.wire.android.ui.common.button.IconAlignment
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions

@Composable
fun SelectParticipantsButtonsAlwaysEnabled(
    count: Int = 0,
    mainButtonText: String,
    elevation: Dp = MaterialTheme.wireDimensions.bottomNavigationShadowElevation,
    modifier: Modifier = Modifier
        .padding(horizontal = dimensions().spacing16x)
        .height(dimensions().groupButtonHeight)
        .fillMaxWidth(),
    onMoreButtonClick: (() -> Unit)? = null,
    onMainButtonClick: () -> Unit,
) {
    SelectParticipantsButtonsRow(
        count = count,
        mainButtonText = mainButtonText,
        shouldAllowNoSelectionContinue = true,
        elevation = elevation,
        modifier = modifier,
        onMoreButtonClick = onMoreButtonClick,
        onMainButtonClick = onMainButtonClick
    )
}

@Composable
fun SelectParticipantsButtonsRow(
    count: Int = 0,
    mainButtonText: String,
    elevation: Dp = MaterialTheme.wireDimensions.bottomNavigationShadowElevation,
    modifier: Modifier = Modifier
        .padding(horizontal = dimensions().spacing16x)
        .height(dimensions().groupButtonHeight)
        .fillMaxWidth(),
    onMoreButtonClick: (() -> Unit)? = null,
    onMainButtonClick: () -> Unit,
) {
    SelectParticipantsButtonsRow(
        count = count,
        mainButtonText = mainButtonText,
        shouldAllowNoSelectionContinue = false,
        elevation = elevation,
        modifier = modifier,
        onMoreButtonClick = onMoreButtonClick,
        onMainButtonClick = onMainButtonClick
    )
}

@Composable
fun SendContentButton(
    mainButtonText: String,
    count: Int,
    onMainButtonClick: () -> Unit,
) {
    SelectParticipantsButtonsRow(
        showTotalSelectedItemsCount = false,
        count = count,
        leadingIcon = {
            Image(
                painter = painterResource(id = R.drawable.ic_send),
                contentDescription = null,
                modifier = Modifier.padding(end = dimensions().spacing12x),
                colorFilter = ColorFilter.tint(
                    if (count > 0) colorsScheme().onPrimaryButtonEnabled else colorsScheme().onPrimaryButtonDisabled
                )
            )
        },
        mainButtonText = mainButtonText,
        shouldAllowNoSelectionContinue = false,
        onMainButtonClick = onMainButtonClick
    )
}

@Composable
private fun SelectParticipantsButtonsRow(
    showTotalSelectedItemsCount: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    count: Int = 0,
    mainButtonText: String,
    shouldAllowNoSelectionContinue: Boolean = true,
    elevation: Dp = MaterialTheme.wireDimensions.bottomNavigationShadowElevation,
    modifier: Modifier = Modifier
        .padding(horizontal = dimensions().spacing16x)
        .height(dimensions().groupButtonHeight)
        .fillMaxWidth(),
    onMoreButtonClick: (() -> Unit)? = null,
    onMainButtonClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.wireColorScheme.background,
        shadowElevation = elevation
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier,
        ) {
            val buttonText = if (showTotalSelectedItemsCount) "$mainButtonText ($count)" else mainButtonText
            WirePrimaryButton(
                text = buttonText,
                leadingIcon = leadingIcon,
                onClick = onMainButtonClick,
                state = computeButtonState(count, shouldAllowNoSelectionContinue),
                blockUntilSynced = true,
                modifier = Modifier.weight(1f)
            )
            if (onMoreButtonClick != null) {
                Spacer(Modifier.width(dimensions().spacing8x))
                WireSecondaryButton(
                    onClick = onMoreButtonClick,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_more),
                            contentDescription = stringResource(R.string.content_description_right_arrow),
                            modifier = Modifier
                                .size(dimensions().wireIconButtonSize)
                        )
                    },
                    leadingIconAlignment = IconAlignment.Center,
                    fillMaxWidth = false
                )
            }
        }
    }
}

private fun computeButtonState(count: Int = 0, shouldAllowNoSelectionContinue: Boolean): WireButtonState {
    return when {
        shouldAllowNoSelectionContinue -> WireButtonState.Default
        count > 0 -> WireButtonState.Default
        else -> WireButtonState.Disabled
    }
}

@Preview
@Composable
fun PreviewSelectParticipantsButtonsRow() {
    SelectParticipantsButtonsRow(count = 3, mainButtonText = "Continue", onMainButtonClick = {}, onMoreButtonClick = {})
}

@Preview
@Composable
fun PreviewSelectParticipantsButtonsRowWithoutMoreButton() {
    SelectParticipantsButtonsRow(count = 3, mainButtonText = "Continue", onMainButtonClick = {})
}

@Preview
@Composable
fun PreviewSelectParticipantsButtonsRowDisabledButton() {
    SelectParticipantsButtonsRow(count = 0, mainButtonText = "Continue", onMainButtonClick = {})
}

@Preview
@Composable
fun PreviewSendButtonsRowDisabledButton() {
    SendContentButton(mainButtonText = "Send", count = 0) {}
}

@Preview
@Composable
fun PreviewSendButtonsRowEnabledButton() {
    SendContentButton(mainButtonText = "Send", count = 1) {}
}
