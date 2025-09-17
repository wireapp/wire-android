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

private const val SELF_BUBBLE_OPACITY = 0.5F
