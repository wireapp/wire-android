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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import com.wire.android.ui.home.conversations.model.UIMention

class MentionVisualTransformation(
    val color: Color,
    val mentions: List<UIMention>
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val styledText = buildAnnotatedString {
            var lastIndex = 0
            text.takeIf { it.isNotEmpty() }?.let {
                mentions.forEach { mention ->
                    // Append the text before the mention
                    append(text.subSequence(lastIndex, mention.start))
                    // Apply the style to the mention
                    withStyle(style = SpanStyle(color = color, fontWeight = FontWeight.Bold)) {
                        append(text.subSequence(mention.start, mention.start + mention.length))
                    }
                    lastIndex = mention.start + mention.length
                }
            }
            // Append the remaining text after the last mention
            append(text.subSequence(lastIndex, text.length))
        }
        return TransformedText(styledText, offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset
            override fun transformedToOriginal(offset: Int): Int = offset
        })
    }
}
