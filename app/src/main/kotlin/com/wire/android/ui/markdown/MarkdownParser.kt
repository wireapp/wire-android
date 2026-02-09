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
package com.wire.android.ui.markdown

import org.commonmark.node.Document
import org.commonmark.parser.IncludeSourceSpans
import org.commonmark.parser.Parser

object MarkdownParser {

    private val parser = Parser.builder()
        .extensions(MarkdownConstants.supportedExtensions)
        .includeSourceSpans(IncludeSourceSpans.BLOCKS)
        .build()

    // We preserve blank lines across *all* block types by using source spans:
    // CommonMark collapses empty lines into block boundaries, so we count
    // blank input lines between top-level blocks and insert spacer paragraphs
    // (Break-only) to render the exact number of empty lines in the UI.
    // We then disable bottom padding on blocks followed by a spacer to avoid
    // double spacing (padding + spacer).
    fun parse(text: String): MarkdownNode.Document {
        val documentNode = parser.parse(text) as Document
        val markdownChildren = mutableListOf<MarkdownNode.Block>()

        // Walk top-level blocks in source order and optionally insert spacer paragraphs between them.
        val blockNodes = collectTopLevelBlocks(documentNode)
        val blankLineCounts = countBlankLinesBetweenBlocks(documentNode, text)
        blockNodes.forEachIndexed { index, block ->
            // Convert each CommonMark top-level block into our UI model.
            markdownChildren.add(block.toContent() as MarkdownNode.Block)
            if (index < blankLineCounts.size) {
                val blankLines = blankLineCounts[index]
                if (blankLines > 0) {
                    // Insert a spacer block to render the exact number of blank lines.
                    markdownChildren.add(createSpacerParagraph(blankLines))
                }
            }
        }

        return MarkdownNode.Document(
            adjustPaddingForSpacers(markdownChildren)
        )
    }

    private fun collectTopLevelBlocks(document: Document): List<org.commonmark.node.Node> {
        // Collect direct children (top-level blocks) because source spans are per block.
        val blocks = mutableListOf<org.commonmark.node.Node>()
        var child = document.firstChild
        while (child != null) {
            blocks.add(child)
            child = child.next
        }
        return blocks
    }

    private fun countBlankLinesBetweenBlocks(document: Document, text: String): List<Int> {
        // Split input by lines so we can count the blank lines between block spans.
        val lines = text.split("\n")
        val blocks = collectTopLevelBlocks(document)
        if (blocks.size < 2) return emptyList()

        val blankCounts = mutableListOf<Int>()
        for (index in 0 until blocks.size - 1) {
            // Source spans point to the lines covered by each block in the original input.
            val currentSpan = blocks[index].sourceLineRange()
            val nextSpan = blocks[index + 1].sourceLineRange()
            blankCounts.add(countBlankLinesBetween(lines, currentSpan, nextSpan))
        }
        return blankCounts
    }

    private fun countBlankLinesBetween(
        lines: List<String>,
        currentSpan: Pair<Int, Int>?,
        nextSpan: Pair<Int, Int>?
    ): Int {
        val start = currentSpan?.second?.plus(1)
        val end = nextSpan?.first?.minus(1)
        val safeEnd = if (start != null && end != null) {
            calculateSafeEnd(start, end, lines.size)
        } else {
            null
        }

        return if (start == null || safeEnd == null) {
            0
        } else {
            lines.subList(start, safeEnd + 1).count { it.isBlank() }
        }
    }

    private fun calculateSafeEnd(start: Int, end: Int, linesSize: Int): Int? {
        var safeEnd: Int? = null
        if (start <= end && start < linesSize) {
            safeEnd = minOf(end, linesSize - 1)
        }
        return safeEnd
    }

    private fun org.commonmark.node.Node.sourceLineRange(): Pair<Int, Int>? {
        // Reduce all spans to a min/max line range for the block.
        val spans = sourceSpans
        if (spans.isEmpty()) return null
        var minLine = Int.MAX_VALUE
        var maxLine = Int.MIN_VALUE
        for (span in spans) {
            minLine = minOf(minLine, span.lineIndex)
            maxLine = maxOf(maxLine, span.lineIndex)
        }
        return if (minLine == Int.MAX_VALUE || maxLine == Int.MIN_VALUE) null else Pair(minLine, maxLine)
    }

    private fun createSpacerParagraph(blankLines: Int): MarkdownNode.Block.Paragraph {
        val breaks = List(blankLines) { MarkdownNode.Inline.Break() }
        return MarkdownNode.Block.Paragraph(breaks, isParentDocument = false)
    }

    private fun adjustPaddingForSpacers(children: List<MarkdownNode.Block>): List<MarkdownNode.Block> {
        if (children.isEmpty()) return children
        val adjusted = mutableListOf<MarkdownNode.Block>()
        for (index in children.indices) {
            val block = children[index]
            // If a spacer follows, suppress padding on the current block to avoid double spacing.
            val nextIsSpacer = index + 1 < children.size && children[index + 1].isSpacerParagraph()
            adjusted.add(if (nextIsSpacer) block.withParentDocument(false) else block)
        }
        return adjusted
    }

    private fun MarkdownNode.Block.isSpacerParagraph(): Boolean {
        return this is MarkdownNode.Block.Paragraph && children.all { it is MarkdownNode.Inline.Break }
    }

    private fun MarkdownNode.Block.withParentDocument(isParentDocument: Boolean): MarkdownNode.Block {
        return when (this) {
            is MarkdownNode.Block.Heading -> copy(isParentDocument = isParentDocument)
            is MarkdownNode.Block.Paragraph -> copy(isParentDocument = isParentDocument)
            is MarkdownNode.Block.BlockQuote -> copy(isParentDocument = isParentDocument)
            is MarkdownNode.Block.ListBlock.Bullet -> copy(isParentDocument = isParentDocument)
            is MarkdownNode.Block.ListBlock.Ordered -> copy(isParentDocument = isParentDocument)
            is MarkdownNode.Block.ListItem -> copy(isParentDocument = isParentDocument)
            is MarkdownNode.Block.IntendedCode -> copy(isParentDocument = isParentDocument)
            is MarkdownNode.Block.FencedCode -> copy(isParentDocument = isParentDocument)
            is MarkdownNode.Block.Table -> copy(isParentDocument = isParentDocument)
            is MarkdownNode.Block.TableContent.Head -> copy(isParentDocument = isParentDocument)
            is MarkdownNode.Block.TableContent.Body -> copy(isParentDocument = isParentDocument)
            is MarkdownNode.Block.ThematicBreak -> copy(isParentDocument = isParentDocument)
        }
    }
}
