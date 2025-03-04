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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.model.ClickBlockParams
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.button.wireSecondaryButtonColors
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.message.SelfDeletionTimer
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Composable
fun SendContentButton(
    mainButtonText: String,
    count: Int,
    onMainButtonClick: () -> Unit,
    onSelfDeletionTimerClicked: () -> Unit,
    modifier: Modifier = Modifier,
    selfDeletionTimer: SelfDeletionTimer = SelfDeletionTimer.Disabled
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(horizontal = dimensions().spacing16x)
            .height(dimensions().groupButtonHeight),
    ) {
        WirePrimaryButton(
            text = mainButtonText,
            onClick = onMainButtonClick,
            leadingIcon = {
                Image(
                    painter = painterResource(id = R.drawable.ic_send),
                    contentDescription = null,
                    modifier = Modifier.padding(end = dimensions().spacing12x),
                    colorFilter = ColorFilter.tint(
                        when {
                            count > 0 -> colorsScheme().onPrimaryButtonEnabled
                            else -> colorsScheme().onPrimaryButtonDisabled
                        }
                    )
                )
            },
            state = if (count > 0) WireButtonState.Default else WireButtonState.Disabled,
            clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
            modifier = Modifier
                .weight(1f)
        )
        SelfDeletionTimerIcon(
            selfDeletionTimer = selfDeletionTimer,
            onSelfDeletionTimerClicked = onSelfDeletionTimerClicked,
            isDisabled = count == 0
        )
    }
}


@Composable
fun SelfDeletionTimerIcon(
    selfDeletionTimer: SelfDeletionTimer,
    isDisabled: Boolean,
    modifier: Modifier = Modifier,
    onSelfDeletionTimerClicked: () -> Unit
) {
    val isSelected = selfDeletionTimer is SelfDeletionTimer.Enabled && selfDeletionTimer.duration != null
    Box(
        modifier = modifier
            .padding(start = dimensions().spacing16x)
            .size(dimensions().spacing48x)
            .clip(RoundedCornerShape(size = dimensions().onMoreOptionsButtonCornerRadius))
    ) {
        WireSecondaryButton(
            leadingIcon = {
                Image(
                    painter = painterResource(id = R.drawable.ic_timer),
                    contentDescription = stringResource(id = R.string.content_description_self_deletion_selector_button),
                    colorFilter = ColorFilter.tint(
                        when {
                            isDisabled -> colorsScheme().onPrimaryButtonDisabled
                            isSelected -> colorsScheme().onSecondaryButtonSelected
                            else -> colorsScheme().onSecondaryButtonEnabled
                        }
                    )
                )
            },
            state = when {
                isDisabled -> WireButtonState.Disabled
                isSelected -> WireButtonState.Selected
                else -> WireButtonState.Default
            },
            colors = wireSecondaryButtonColors().copy(
                disabled = colorsScheme().primaryButtonDisabled,
                selected = colorsScheme().secondaryButtonSelected,
                selectedOutline = colorsScheme().primaryButtonEnabled,
            ),
            onClick = onSelfDeletionTimerClicked,
            shape = RoundedCornerShape(size = dimensions().onMoreOptionsButtonCornerRadius),
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSelfDeletionTimerButton() = WireTheme {
    SelfDeletionTimerIcon(
        selfDeletionTimer = SelfDeletionTimer.Enabled(duration = null),
        isDisabled = false,
        modifier = Modifier,
        onSelfDeletionTimerClicked = {}
    )
}


@PreviewMultipleThemes
@Composable
fun PreviewSendContentWithSelfDeletionButton() {
    WireTheme {
        SendContentButton(
            mainButtonText = "Send",
            count = 1,
            onMainButtonClick = {},
            onSelfDeletionTimerClicked = {},
            selfDeletionTimer = SelfDeletionTimer.Enabled(ZERO)
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSendContentWithSelfDeletionDisabledButton() {
    WireTheme {
        SendContentButton(
            mainButtonText = "Self-deleting messages",
            count = 0,
            onMainButtonClick = {},
            onSelfDeletionTimerClicked = {},
            selfDeletionTimer = SelfDeletionTimer.Enabled(10.toDuration(DurationUnit.SECONDS))
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSendContentWithSelfDeletionSelectedButton() {
    WireTheme {
        SendContentButton(
            mainButtonText = "Self-deleting messages",
            count = 1,
            onMainButtonClick = {},
            onSelfDeletionTimerClicked = {},
            selfDeletionTimer = SelfDeletionTimer.Enabled(10.toDuration(DurationUnit.SECONDS))
        )
    }
}
