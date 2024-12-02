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

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.ui.home.conversations.model.UIMention

/**
 * Manages the updates to a `TextFieldValue` based on changes in the text and mentions.
 */
class MentionUpdateCoordinator(
    private val mentionAdjuster: MentionAdjuster = MentionAdjuster(),
    private val selectionManager: MentionSelectionManager = MentionSelectionManager()
) {
    @Suppress("ReturnCount")
    fun handle(
        oldTextFieldValue: TextFieldValue,
        newTextFieldValue: TextFieldValue,
        mentions: List<UIMention>,
        updateMentions: (List<UIMention>) -> Unit
    ): TextFieldValue {
        if (newTextFieldValue.text.isEmpty()) {
            updateMentions(emptyList())
            return newTextFieldValue
        }

        // If there are no mentions, simply return the new TextFieldValue.
        if (mentions.isEmpty()) {
            return newTextFieldValue
        }

        val deletedLength = oldTextFieldValue.text.length - newTextFieldValue.text.length
        val addedLength = newTextFieldValue.text.length - oldTextFieldValue.text.length

        when {
            deletedLength > 0 -> {
                val result = mentionAdjuster.adjustMentionsForDeletion(
                    mentions = mentions,
                    deletedLength = deletedLength,
                    text = newTextFieldValue.text,
                    selection = newTextFieldValue.selection
                )
                updateMentions(result.first)
                return newTextFieldValue.copy(selection = result.second)
            }

            addedLength > 0 -> {
                val result =
                    mentionAdjuster.adjustMentionsForInsertion(
                        mentions = mentions,
                        text = newTextFieldValue.text,
                        selection = newTextFieldValue.selection,
                        addedLength = addedLength
                    )
                updateMentions(result.first)
                return newTextFieldValue.copy(selection = result.second)
            }
        }

        // To select the mention if the user clicks on it
        val newSelection = if (oldTextFieldValue.text == newTextFieldValue.text) {
            selectionManager.updateSelectionForMention(
                oldTextFieldValue.selection,
                newTextFieldValue.selection,
                mentions
            )
        } else {
            newTextFieldValue.selection
        }

        return newTextFieldValue.copy(selection = newSelection)
    }
}
