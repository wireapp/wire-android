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
package com.wire.android.ui.home.messagecomposer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.messagecomposer.state.MessageComposerState

@Composable
fun InActiveMessageComposer(
    messageListContent: @Composable () -> Unit,
    onTransistionToActive: (Boolean) -> Unit,
    inActiveComposerState: MessageComposerState.InActive
) {
    Surface(color = colorsScheme().messageComposerBackgroundColor) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            Box(
                Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {},
                            onDoubleTap = { /* Called on Double Tap */ },
                            onLongPress = { /* Called on Long Press */ },
                            onTap = { /* Called on Tap */ }
                        )
                    }
                    .background(color = colorsScheme().backgroundVariant)
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                messageListContent()
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Box(modifier = Modifier.padding(start = dimensions().spacing8x)) {
                    AdditionalOptionButton(
                        isSelected = false,
                        isEnabled = true,
                        onClick = { onTransistionToActive(true) }
                    )
                }

                Text(inActiveComposerState.messageComposition.messageText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clickable { onTransistionToActive(false) }
                )
            }
        }
    }
}
