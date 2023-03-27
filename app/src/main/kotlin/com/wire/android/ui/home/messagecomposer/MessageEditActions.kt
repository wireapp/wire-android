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

package com.wire.android.ui.home.messagecomposer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.model.ClickBlockParams
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryIconButton
import com.wire.android.ui.common.button.WireTertiaryIconButton
import com.wire.android.ui.common.button.wireSendPrimaryButtonColors
import com.wire.android.ui.common.dimensions

@Composable
fun MessageEditActions(
    editButtonEnabled: Boolean = false,
    onEditSaveButtonClicked: () -> Unit = { },
    onEditCancelButtonClicked: () -> Unit = { },
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize()
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
                minHeight = dimensions().spacing40x,
                minWidth = dimensions().spacing40x,
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
                minHeight = dimensions().spacing40x,
                minWidth = dimensions().spacing40x,
            )
        }
    }
}

@Preview
@Composable
fun PreviewMessageEditActionsEnabled() {
    MessageEditActions(true, {}, {})
}
@Preview
@Composable
fun PreviewMessageEditActionsDisabled() {
    MessageEditActions(false, {}, {})
}
