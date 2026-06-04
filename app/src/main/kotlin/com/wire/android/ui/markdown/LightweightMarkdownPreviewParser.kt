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

package com.wire.android.ui.markdown

private const val MAX_PREVIEW_MARKDOWN_CHARS = 200
private val MARKDOWN_PREVIEW_INLINE_TRIGGERS = charArrayOf('*', '_', '`', '[', '!', '~')

internal fun String.previewMarkdownSource(): String {
    val newlineIndex = indexOf('\n').takeIf { it >= 0 } ?: length
    val previewEnd = minOf(newlineIndex, MAX_PREVIEW_MARKDOWN_CHARS)
    return substring(0, previewEnd).replace(MarkdownConstants.NON_BREAKING_SPACE, " ")
}

internal fun String.toLightweightMarkdownPreview(): MarkdownPreview? {
    return previewMarkdownSource().toLightweightMarkdownPreviewFromSource()
}

internal fun String.toLightweightMarkdownPreviewFromSource(): MarkdownPreview? {
    if (isBlank() || !hasPreviewMarkdownTrigger()) {
        return null
    }
    return MarkdownParser.parsePreview(this)
        ?.takeIf { it.children.hasVisiblePreviewText() }
}

internal fun String.hasPreviewMarkdownTrigger(): Boolean =
    indexOfAny(MARKDOWN_PREVIEW_INLINE_TRIGGERS) >= 0 || firstNonWhitespaceCharCanStartMarkdown()

private fun String.firstNonWhitespaceCharCanStartMarkdown(): Boolean {
    val firstContentIndex = indexOfFirst { !it.isWhitespace() }
    if (firstContentIndex == -1) return false
    return when (this[firstContentIndex]) {
        '#', '>', '-', '+', '|' -> true
        in '0'..'9' -> isOrderedListMarker(firstContentIndex)
        else -> false
    }
}

private fun String.isOrderedListMarker(startIndex: Int): Boolean {
    var index = startIndex
    while (index < length && this[index].isDigit()) {
        index++
    }
    return index > startIndex && index + 1 < length && this[index] in ".)" && this[index + 1].isWhitespace()
}

private fun List<MarkdownNode.Inline>.hasVisiblePreviewText(): Boolean {
    return any { inline ->
        when (inline) {
            is MarkdownNode.Inline.Text -> inline.literal.isNotBlank()
            is MarkdownNode.Inline.Code -> inline.literal.isNotBlank()
            is MarkdownNode.Inline.Break -> false
            is MarkdownNode.Inline.Emphasis -> inline.children.hasVisiblePreviewText()
            is MarkdownNode.Inline.StrongEmphasis -> inline.children.hasVisiblePreviewText()
            is MarkdownNode.Inline.Strikethrough -> inline.children.hasVisiblePreviewText()
            is MarkdownNode.Inline.Link -> inline.children.hasVisiblePreviewText() || inline.destination.isNotBlank()
            is MarkdownNode.Inline.Image -> inline.children.hasVisiblePreviewText() || inline.destination.isNotBlank()
        }
    }
}
