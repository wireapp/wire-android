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

package com.wire.android.ui.home.conversations.messages

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.button.wireSecondaryButtonColors
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.messages.item.interceptCombinedClickable
import com.wire.android.ui.theme.Accent
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun ReactionPill(
    emoji: String,
    count: Int,
    isOwn: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null
) {

    val strokeColor = if (isOwn) {
        MaterialTheme.wireColorScheme.secondaryButtonSelectedOutline
    } else {
        MaterialTheme.wireColorScheme.primaryButtonDisabled
    }

    val backgroundColor = if (isOwn) {
        MaterialTheme.wireColorScheme.secondaryButtonSelected
    } else {
        MaterialTheme.wireColorScheme.surface
    }

    val textColor = if (isOwn) {
        MaterialTheme.wireColorScheme.primary
    } else {
        MaterialTheme.wireColorScheme.secondaryText
    }

    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize provides Dp.Unspecified
    ) {
        WireSecondaryButton(
            modifier = modifier
                .interceptCombinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = LocalIndication.current,
                    onLongPress = onLongClick
                ),
            onClick = onTap,
            shape = RoundedCornerShape(borderRadius),
            contentPadding = PaddingValues(
                horizontal = dimensions().spacing8x,
                vertical = dimensions().spacing4x
            ),
            colors = wireSecondaryButtonColors().copy(
                enabled = backgroundColor,
                enabledOutline = strokeColor,
            ),
            fillMaxWidth = false,
            minClickableSize = DpSize(minDimension, minDimension),
            borderWidth = borderStrokeWidth,
            minSize = DpSize(minDimension, minDimension),
            leadingIcon = {
                Text(
                    emoji,
                    style = TextStyle(fontSize = reactionFontSize)
                )
                Spacer(modifier = Modifier.width(dimensions().spacing4x))
                Text(
                    count.toString(),
                    style = MaterialTheme.wireTypography.label02,
                    color = textColor
                )
            },
        )
    }
}

@PreviewMultipleThemes
@Composable
fun ReactionPillPreview() = WireTheme(accent = Accent.Unknown) {
    ReactionPill(
        emoji = "👍",
        count = 5,
        isOwn = false,
        onTap = {}
    )
}

@PreviewMultipleThemes
@Composable
fun ReactionOwnPillPreview() = WireTheme(accent = Accent.Amber) {
    ReactionPill(
        emoji = "👍",
        count = 5,
        isOwn = true,
        onTap = {}
    )
}

private val minDimension = 1.dp
private val borderRadius = 12.dp
private val borderStrokeWidth = 1.dp
private val reactionFontSize = 12.sp
