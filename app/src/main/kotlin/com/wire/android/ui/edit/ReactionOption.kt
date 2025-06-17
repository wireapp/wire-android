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
package com.wire.android.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wire.android.R
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.emoji.EmojiPickerBottomSheet
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReactionOption(
    onReactionClick: (emoji: String) -> Unit,
    modifier: Modifier = Modifier,
    emojiFontSize: TextUnit = 28.sp
) {
    val emojiPickerState = rememberWireModalSheetState<Unit>(skipPartiallyExpanded = false)
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Column(modifier = modifier) {
            Row {
                Spacer(modifier = Modifier.width(dimensions().spacing8x))
                Text(
                    stringResource(R.string.label_reactions).uppercase(),
                    style = MaterialTheme.wireTypography.label01
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("👍", "🙂", "❤️", "☹️", "👎").forEach { emoji ->
                    CompositionLocalProvider(
                        LocalMinimumInteractiveComponentSize provides Dp.Unspecified,
                    ) {
                        Button(
                            onClick = {
                                onReactionClick(emoji)
                            },
                            modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp),
                            contentPadding = PaddingValues(dimensions().spacing8x),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.wireColorScheme.surface,
                                contentColor = MaterialTheme.wireColorScheme.secondaryButtonSelectedOutline
                            )
                        ) {
                            Text(emoji, style = TextStyle(fontSize = emojiFontSize))
                        }
                    }
                }
                IconButton(
                    onClick = {
                        emojiPickerState.show(Unit)
                    },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_more_emojis),
                        contentDescription = stringResource(R.string.content_description_more_emojis)
                    )
                }
            }
        }
    }
    EmojiPickerBottomSheet(
        sheetState = emojiPickerState,
        onEmojiSelected = { emoji, _ ->
            emojiPickerState.hide()
            onReactionClick(emoji)
        }
    )
}

@PreviewMultipleThemes
@Composable
private fun BasePreview() = WireTheme {
    ReactionOption(onReactionClick = {})
}
