/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.feature.meetings.ui.create

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle

/**
 * Custom [OutputTransformation] to handle dynamic truncation of a comma-separated list of items (e.g., participant names) based on
 * available width. It iteratively reduces the number of visible items and appends a dynamic suffix (e.g., "and X more") until the text
 * fits within the specified width. The transformation ensures that the visible items and the ellipsis are styled in main color,
 * while the dynamic suffix is styled in different color.
 * Example: "Alice, Bob, Charlie... +2 more"
 */
class TextListTruncationTransformation(
    private val availableWidthPx: Int,
    private val textMeasurer: TextMeasurer,
    private val textStyle: TextStyle,
    private val textColor: Color,
    private val suffixColor: Color,
    private val provideSuffixText: (Int) -> String,
    private val separator: String = ", ",
    private val ellipsis: String = "... ",
) : OutputTransformation {
    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "ReturnCount")
    override fun TextFieldBuffer.transformOutput() {
        if (availableWidthPx <= 0) return

        val fullText = asCharSequence().toString()
        if (fullText.isEmpty()) return

        val allItems = fullText.split(separator).map { it.trim() }.filter { it.isNotEmpty() }
        if (allItems.isEmpty()) return

        fun visibleNames(visibleCount: Int): String = allItems.take(visibleCount).joinToString(separator)
        fun suffixText(remainingCount: Int): String = if (remainingCount > 0) provideSuffixText(remainingCount) else ""

        for (i in allItems.size downTo 0) {
            val ellipsis = if (i < allItems.size) ellipsis else ""
            val text = visibleNames(i) + ellipsis
            val suffix = suffixText(allItems.size - i)
            if (textMeasurer.measure(text + suffix, textStyle).size.width <= availableWidthPx) {
                if (i < allItems.size) {
                    val currentItem = allItems[i]
                    for (j in currentItem.length downTo 1) {
                        val truncatedItem = currentItem.take(j)
                        val separatorBeforeTruncatedItem = if (i > 0) separator else ""
                        val ellipsisAfterTruncatedItem = if (i + 1 < allItems.size || j < currentItem.length) ellipsis else ""
                        val textWithTruncated = visibleNames(i) + separatorBeforeTruncatedItem + truncatedItem + ellipsisAfterTruncatedItem
                        val suffixWithoutTruncated = suffixText(allItems.size - i - 1)
                        if (textMeasurer.measure(textWithTruncated + suffixWithoutTruncated, textStyle).size.width <= availableWidthPx) {
                            applyVisuals(this, textWithTruncated, suffixWithoutTruncated)
                            return
                        }
                    }
                }
                applyVisuals(this, text, suffix)
                return
            }
        }
    }

    private fun applyVisuals(buffer: TextFieldBuffer, mainTextWithEllipsis: String, suffix: String) {
        buffer.replace(0, buffer.length, mainTextWithEllipsis + suffix)
        buffer.addStyle(SpanStyle(color = textColor), 0, mainTextWithEllipsis.length)
        buffer.addStyle(SpanStyle(color = suffixColor), mainTextWithEllipsis.length, mainTextWithEllipsis.length + suffix.length)
    }
}
