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

package com.wire.android.ui.home.messagecomposer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.wire.android.R
import com.wire.android.model.ClickBlockParams
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryIconButton
import com.wire.android.ui.common.button.WireTertiaryButton
import com.wire.android.ui.common.button.WireTertiaryIconButton
import com.wire.android.ui.common.button.wireSendPrimaryButtonColors
import com.wire.android.ui.common.button.wireTertiaryButtonColors
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.typography
import com.wire.android.ui.home.conversations.selfdeletion.SelfDeletionMapper.toSelfDeletionDuration
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.message.SelfDeletionTimer
import kotlin.time.Duration.Companion.minutes

@Composable
fun MessageSendActions(
    sendButtonEnabled: Boolean,
    onSendButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    SendButton(
        isEnabled = sendButtonEnabled,
        onSendButtonClicked = onSendButtonClicked,
        modifier = modifier,
    )
}

@Composable
fun SelfDeletingActions(
    selfDeletionTimer: SelfDeletionTimer,
    sendButtonEnabled: Boolean,
    onSendButtonClicked: () -> Unit,
    onChangeSelfDeletionClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        WireTertiaryButton(
            minSize = MaterialTheme.wireDimensions.buttonCircleMinSize,
            minClickableSize = MaterialTheme.wireDimensions.buttonCircleMinSize,
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = dimensions().spacing4x, vertical = dimensions().spacing8x),
            onClick = onChangeSelfDeletionClicked,
            text = selfDeletionTimer.duration.toSelfDeletionDuration().shortLabel.asString(),
            textStyle = typography().label02,
            fillMaxWidth = false,
            state = if (!selfDeletionTimer.isEnforced) WireButtonState.Default else WireButtonState.Disabled,
            colors = wireTertiaryButtonColors().copy(onEnabled = colorsScheme().primary, onDisabled = colorsScheme().primary),
        )
        WirePrimaryIconButton(
            onButtonClicked = onSendButtonClicked,
            iconResource = R.drawable.ic_timer,
            contentDescription = R.string.content_description_send_button,
            state = if (sendButtonEnabled) WireButtonState.Default else WireButtonState.Disabled,
            shape = RoundedCornerShape(dimensions().spacing20x),
            colors = wireSendPrimaryButtonColors(),
            clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = false),
            minSize = MaterialTheme.wireDimensions.buttonCircleMinSize,
            minClickableSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
        )
    }
}

@Composable
fun MessageEditActions(
    modifier: Modifier = Modifier,
    editButtonEnabled: Boolean = false,
    onEditSaveButtonClicked: () -> Unit = { },
    onEditCancelButtonClicked: () -> Unit = { }
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensions().spacing64x)
    ) {
        Box( // we need to wrap it because button is smaller than minimum touch size so compose will add paddings to it to be 48dp anyway
            modifier = Modifier.size(width = dimensions().spacing64x, height = dimensions().spacing56x),
            contentAlignment = Alignment.CenterEnd
        ) {
            WireTertiaryIconButton(
                onButtonClicked = onEditCancelButtonClicked,
                iconResource = R.drawable.ic_close,
                contentDescription = R.string.content_description_close_button,
                shape = CircleShape,
                minSize = MaterialTheme.wireDimensions.buttonCircleMinSize,
                minClickableSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier.size(width = dimensions().spacing64x, height = dimensions().spacing56x),
            contentAlignment = Alignment.CenterStart
        ) {
            WirePrimaryIconButton(
                onButtonClicked = onEditSaveButtonClicked,
                iconResource = R.drawable.ic_check_tick,
                contentDescription = R.string.content_description_edit_the_message,
                state = if (editButtonEnabled) WireButtonState.Default else WireButtonState.Disabled,
                colors = wireSendPrimaryButtonColors(),
                clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = false),
                shape = CircleShape,
                minSize = MaterialTheme.wireDimensions.buttonCircleMinSize,
                minClickableSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
            )
        }
    }
}

@Composable
private fun SendButton(
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
    onSendButtonClicked: () -> Unit
) {
    WirePrimaryIconButton(
        modifier = modifier,
        onButtonClicked = onSendButtonClicked,
        iconResource = R.drawable.ic_send,
        contentDescription = R.string.content_description_send_button,
        state = if (isEnabled) WireButtonState.Default else WireButtonState.Disabled,
        shape = RoundedCornerShape(dimensions().spacing20x),
        colors = wireSendPrimaryButtonColors(),
        clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = false),
        minSize = MaterialTheme.wireDimensions.buttonCircleMinSize,
        minClickableSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewMessageEditActionsEnabled() = WireTheme {
    MessageEditActions(Modifier, true, {}, {})
}

@PreviewMultipleThemes
@Composable
fun PreviewMessageEditActionsDisabled() = WireTheme {
    MessageEditActions(Modifier, false, {}, {})
}

@PreviewMultipleThemes
@Composable
fun PreviewMessageSelfDeletingActionsEnabled() = WireTheme {
    SelfDeletingActions(SelfDeletionTimer.Enabled(1.minutes), true, {}, {}, Modifier)
}

@PreviewMultipleThemes
@Composable
fun PreviewMessageSelfDeletingActionsDisabled() = WireTheme {
    SelfDeletingActions(SelfDeletionTimer.Enabled(1.minutes), false, {}, {}, Modifier)
}

@PreviewMultipleThemes
@Composable
fun PreviewMessageSendActionsEnabled() = WireTheme {
    MessageSendActions(true, {})
}

@PreviewMultipleThemes
@Composable
fun PreviewMessageSendActionsDisabled() = WireTheme {
    MessageSendActions(false, {})
}
