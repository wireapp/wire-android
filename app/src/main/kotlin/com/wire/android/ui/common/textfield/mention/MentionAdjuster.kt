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
package com.wire.android.ui.common.textfield.mention

import androidx.compose.ui.text.TextRange
import com.wire.android.ui.home.conversations.model.UIMention

/**
 * Adjusts mentions based on changes in the text.
 */
class MentionAdjuster {

    /**
     * Adjusts mentions based on the deleted text.
     * @param mentions The list of mentions in the text.
     * @param deletedLength The length of the deleted text.
     * @param text The new text after deletion.
     * @param selection The current selection.
     */
    @Suppress("NestedBlockDepth")
    fun adjustMentionsForDeletion(
        mentions: List<UIMention>,
        deletedLength: Int,
        text: String,
        selection: TextRange
    ): Pair<List<UIMention>, TextRange> {
        val updatedMentions = mutableListOf<UIMention>()
        var newSelection = selection

        mentions.forEach { mention ->
            if (selection.start >= mention.start + mention.length) {
                // No change for mentions that are before the deleted text.
                updatedMentions.add(mention)
                // if the cursor is at the end of the mention, select te mention
                if (mention.start + mention.length == selection.end) {
                    newSelection = TextRange(mention.start, mention.start + mention.length)
                }
            } else {
                // Handle mentions that were affected by the deletion and adjusting their start position.
                val newStart = mention.start - deletedLength
                if (newStart >= 0) {
                    val mentionSubstring = text.substring(newStart, newStart + mention.length)
                    if (mentionSubstring == mention.handler) {
                        updatedMentions.add(mention.copy(start = newStart))
                    }
                }
            }
        }

        return Pair(updatedMentions, newSelection)
    }

    /**
     * Adjusts mentions based on the inserted text.
     * @param mentions The list of mentions in the text.
     * @param text The new text after insertion.
     * @param addedLength The length of the inserted text.
     * @param selection The current selection.
     */
    fun adjustMentionsForInsertion(
        mentions: List<UIMention>,
        text: String,
        selection: TextRange,
        addedLength: Int
    ): Pair<List<UIMention>, TextRange> {
        val updatedMentions = mutableListOf<UIMention>()
        // Adjust mentions based on the inserted text.
        mentions.forEach { mention ->
            val mentionSubstring = text.substring(mention.start, mention.start + mention.length)
            if (mentionSubstring == mention.handler) {
                // No change if the mention text remains the same.
                updatedMentions.add(mention)
            } else {
                // Handle mentions that were affected by the insertion and shift their start position.
                updatedMentions.add(mention.copy(start = mention.start + addedLength))
            }
        }

        return Pair(updatedMentions, selection)
    }
}
