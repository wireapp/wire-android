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
 * Manages the selection for mentions.
 */
class MentionSelectionManager {
    fun updateSelectionForMention(
        oldSelection: TextRange,
        newSelection: TextRange,
        mentions: List<UIMention>
    ): TextRange {
        if (oldSelection != newSelection) {
            mentions.forEach { mention ->
                if (newSelection.isInside(mention)) {
                    return TextRange(mention.start, mention.start + mention.length)
                }
            }
        }
        return newSelection
    }

    /**
     * Extension function to check if the selection is inside the mention's range.
     */
    private fun TextRange.isInside(mention: UIMention): Boolean {
        return this.start in mention.start until mention.start + mention.length &&
                this.end in mention.start until mention.start + mention.length
    }
}
