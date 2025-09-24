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
package com.wire.android.ui.home.conversations.messages.item

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.wire.android.ui.theme.wireColorScheme

enum class MessageStyle {
    BUBBLE_SELF,
    BUBBLE_OTHER,
    NORMAL
}

fun MessageStyle.isBubble(): Boolean = this != MessageStyle.NORMAL

fun MessageStyle.alpha() = when (this) {
    MessageStyle.BUBBLE_SELF -> SELF_BUBBLE_OPACITY
    MessageStyle.BUBBLE_OTHER -> 1F
    MessageStyle.NORMAL -> 1F
}

@Composable
fun MessageStyle.textColor(): Color {
    return when (this) {
        MessageStyle.BUBBLE_SELF -> MaterialTheme.wireColorScheme.onPrimary
        MessageStyle.BUBBLE_OTHER -> MaterialTheme.wireColorScheme.secondaryText
        MessageStyle.NORMAL -> MaterialTheme.wireColorScheme.secondaryText
    }
}

@Composable
fun MessageStyle.onBackground(): Color {
    return when (this) {
        MessageStyle.BUBBLE_SELF -> MaterialTheme.wireColorScheme.onPrimary
        MessageStyle.BUBBLE_OTHER -> MaterialTheme.wireColorScheme.onBackground
        MessageStyle.NORMAL -> MaterialTheme.wireColorScheme.onBackground
    }
}

@Composable
fun MessageStyle.surface(): Color = when (this) {
    MessageStyle.BUBBLE_SELF -> MaterialTheme.wireColorScheme.scrim
    MessageStyle.BUBBLE_OTHER -> MaterialTheme.wireColorScheme.surfaceVariant
    MessageStyle.NORMAL -> MaterialTheme.wireColorScheme.outline
}

@Composable
fun MessageStyle.highlighted(): Color = when (this) {
    MessageStyle.BUBBLE_SELF -> MaterialTheme.wireColorScheme.onPrimary
    MessageStyle.BUBBLE_OTHER -> MaterialTheme.wireColorScheme.primary
    MessageStyle.NORMAL -> MaterialTheme.wireColorScheme.primary
}

@Composable
fun MessageStyle.onNodeBackground(): Color = when (this) {
    MessageStyle.BUBBLE_SELF -> MaterialTheme.wireColorScheme.markdownNodeTextColor
    MessageStyle.BUBBLE_OTHER -> MaterialTheme.wireColorScheme.onBackground
    MessageStyle.NORMAL -> MaterialTheme.wireColorScheme.onBackground
}

@Composable
fun MessageStyle.errorTextStyle(): TextStyle {
    return when (this) {
        MessageStyle.BUBBLE_SELF -> MaterialTheme.typography.bodySmall
        MessageStyle.BUBBLE_OTHER -> MaterialTheme.typography.bodySmall
        MessageStyle.NORMAL -> MaterialTheme.typography.labelSmall
    }
}

@Composable
fun MessageStyle.textAlign(): TextAlign {
    return when (this) {
        MessageStyle.BUBBLE_SELF -> TextAlign.End
        MessageStyle.BUBBLE_OTHER -> TextAlign.Start
        MessageStyle.NORMAL -> TextAlign.Start
    }
}

private const val SELF_BUBBLE_OPACITY = 0.5F
