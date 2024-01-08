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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReactionPill(
    emoji: String,
    count: Int,
    isOwn: Boolean,
    modifier: Modifier = Modifier,
    onTap: () -> Unit
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
        MaterialTheme.wireColorScheme.labelText
    }

    CompositionLocalProvider(
        LocalMinimumInteractiveComponentEnforcement provides false
    ) {
        OutlinedButton(
            onClick = onTap,
            border = BorderStroke(borderStrokeWidth, strokeColor),
            shape = RoundedCornerShape(borderRadius),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = backgroundColor),
            contentPadding = PaddingValues(horizontal = dimensions().spacing8x, vertical = dimensions().spacing4x),
            modifier = modifier.defaultMinSize(minWidth = minDimension, minHeight = minDimension)
        ) {
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
        }
    }
}

private val minDimension = 1.dp
private val borderRadius = 12.dp
private val borderStrokeWidth = 1.dp
private val reactionFontSize = 12.sp
