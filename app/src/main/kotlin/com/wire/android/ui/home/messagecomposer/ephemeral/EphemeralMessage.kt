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
 */
package com.wire.android.ui.home.messagecomposer.ephemeral

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wire.android.R
import com.wire.android.model.ClickBlockParams
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryIconButton
import com.wire.android.ui.common.button.wireSendPrimaryButtonColors
import com.wire.android.ui.common.dimensions

@Composable
fun SelfDeletingActions() {
    Box() {
        Row(Modifier.padding(end = dimensions().spacing8x)) {
//            if (messageComposerState.sendButtonEnabled) {
//                ScheduleMessageButton()
//            }
        }
    }
}

@Composable
private fun SendSelfDeleteButton(
    isEnabled: Boolean,
    onSelfDeleteButtonClicked: () -> Unit
) {
    WirePrimaryIconButton(
        onButtonClicked = onSelfDeleteButtonClicked,
        iconResource = R.drawable.ic_send,
        contentDescription = R.string.content_description_send_button,
        state = if (isEnabled) WireButtonState.Default else WireButtonState.Disabled,
        shape = RoundedCornerShape(dimensions().spacing20x),
        colors = wireSendPrimaryButtonColors(),
        clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = false),
        minHeight = dimensions().spacing40x,
        minWidth = dimensions().spacing40x
    )
}

@Composable
private fun ExpireAfterButton() {
}

