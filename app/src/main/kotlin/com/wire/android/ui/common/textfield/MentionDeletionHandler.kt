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
package com.wire.android.ui.common.textfield

import androidx.compose.ui.text.TextRange

object MentionDeletionHandler {
    fun handle(
        oldText: String,
        newText: String,
        oldSelection: TextRange,
        mentions: List<String>
    ): String {
        if (oldText == newText) {
            // No change in text, only cursor movement, return as is
            return oldText
        }
        for (mention in mentions) {
            // Find the start position of the mention in the text
            val mentionStart = oldText.indexOf(mention)

            if (mentionStart == -1) continue

            val mentionEnd = mentionStart + mention.length

            // Check if the selection (i.e., user's cursor position) is inside the mention's range
            if (oldSelection.start in mentionStart + 1..mentionEnd || oldSelection.end in mentionStart + 1..mentionEnd) {
                // If the user is deleting inside the mention, remove the entire mention
                return oldText.removeRange(mentionStart, mentionEnd)
            }
        }
        return newText
    }
}
