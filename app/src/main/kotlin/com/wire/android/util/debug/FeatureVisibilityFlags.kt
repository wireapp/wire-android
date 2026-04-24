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

package com.wire.android.util.debug

import androidx.compose.runtime.staticCompositionLocalOf

// As a Beta user I don’t want to press on buttons that don’t have functions yet,
// because then I will think something is not working in the app.
//
// ACCEPTANCE CRITERIA
//
// Buttons to remove from the UI:
// Settings → App settings entry
// Settings → Backup and restore information entry
// Conversation view → + icon → Audio messages
// Conversation View → + icon → share location
// Conversation View → Aa icon
// Conversation View → :smile: icon
// Conversation View → GIF icon
// Conversation View → @ icon
// Conversation View → ping icon
// Conversation View → search icon
// User Profile → Edit icon
// long press on text → edit entry
// long press on images → edit
// long press on images → copy

// Those flags can be removed once we set all flags to true :)
object FeatureVisibilityFlags {
    const val AppSettings = false
    const val BackUpSettings = true
    const val AudioMessagesIcon = true
    const val ShareLocationIcon = true
    const val RichTextIcon = true
    const val EmojiIcon = false
    const val GifIcon = false
    const val PingIcon = true
    const val ConversationSearchIcon = false
    const val UserProfileEditIcon = false
    const val MessageEditIcon = true
    const val SearchConversationMessages = true
    const val DrawingIcon = true
}

val LocalFeatureVisibilityFlags = staticCompositionLocalOf { FeatureVisibilityFlags }
