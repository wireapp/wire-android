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
@file:Suppress("ComplexMethod", "TooManyFunctions")

package com.wire.android.ui.markdown

import com.wire.android.appLogger
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableBody
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Document
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.HtmlBlock
import org.commonmark.node.HtmlInline
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.LinkReferenceDefinition
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.node.ThematicBreak

fun String.toMarkdownDocument(): MarkdownNode.Document = MarkdownParser.parse(this)

fun <T : Node> T.toContent(isParentDocument: Boolean = false): MarkdownNode {
    return when (this) {
        is Document -> MarkdownNode.Document(convertChildren<MarkdownNode.Block>())
        is Heading -> MarkdownNode.Block.Heading(convertChildren<MarkdownNode.Inline>(), isParentDocument, this.level)
        is Paragraph -> MarkdownNode.Block.Paragraph(convertChildren<MarkdownNode.Inline>(), isParentDocument)
        is BlockQuote -> MarkdownNode.Block.BlockQuote(convertChildren<MarkdownNode.Block>(), isParentDocument)
        is BulletList -> MarkdownNode.Block.ListBlock.Bullet(convertChildren<MarkdownNode.Block.ListItem>(), isParentDocument, bulletMarker)
        is OrderedList -> {
            val listItems = convertChildren<MarkdownNode.Block.ListItem>()
                .mapIndexed { index, node ->
                    node.copy(orderNumber = this.startNumber + index)
                }
            MarkdownNode.Block.ListBlock.Ordered(listItems, isParentDocument, startNumber, delimiter)
        }

        is FencedCodeBlock -> MarkdownNode.Block.FencedCode(isParentDocument, literal)
        is IndentedCodeBlock -> MarkdownNode.Block.IntendedCode(isParentDocument, literal)
        is HtmlBlock -> MarkdownNode.Block.Paragraph(
            children = listOf(MarkdownNode.Inline.Text(this.literal)),
            isParentDocument
        ) // TODO unsupported html

        is TableBlock -> MarkdownNode.Block.Table(convertChildren<MarkdownNode.Block.TableContent>(), isParentDocument)
        is TableHead -> MarkdownNode.Block.TableContent.Head(convertChildren<MarkdownNode.TableRow>())
        is TableRow -> MarkdownNode.TableRow(convertChildren<MarkdownNode.TableCell>())
        is TableCell -> MarkdownNode.TableCell(convertChildren<MarkdownNode.Inline>())
        is TableBody -> MarkdownNode.Block.TableContent.Body(convertChildren<MarkdownNode.TableRow>())
        is ListItem -> MarkdownNode.Block.ListItem(convertChildren<MarkdownNode.Block>(), orderNumber = 1)
        is Text -> MarkdownNode.Inline.Text(this.literal)
        is StrongEmphasis -> MarkdownNode.Inline.StrongEmphasis(convertChildren<MarkdownNode.Inline>())
        is Emphasis -> MarkdownNode.Inline.Emphasis(convertChildren<MarkdownNode.Inline>())
        is Link -> MarkdownNode.Inline.Link(this.destination, this.title, convertChildren<MarkdownNode.Inline>())
        is Image -> MarkdownNode.Inline.Image(this.destination, this.title, convertChildren<MarkdownNode.Inline>())
        is Code -> MarkdownNode.Inline.Code(this.literal)
        is HtmlInline -> MarkdownNode.Inline.Text(this.literal) // TODO unsupported html
        is ThematicBreak -> MarkdownNode.Block.ThematicBreak(convertChildren<MarkdownNode.Inline>(), isParentDocument)
        is Strikethrough -> MarkdownNode.Inline.Strikethrough(convertChildren<MarkdownNode.Inline>())
        is HardLineBreak, is SoftLineBreak -> MarkdownNode.Inline.Break(convertChildren<MarkdownNode.Inline>())
        is LinkReferenceDefinition -> MarkdownNode.Block.Paragraph(
            listOf(MarkdownNode.Inline.Text("[$label]: $destination $title")),
            isParentDocument
        )

        else -> {
            appLogger.e(
                "Unsupported markdown",
                IllegalArgumentException("Unsupported node type: ${this.javaClass.simpleName}")
            )
            MarkdownNode.Unsupported(isParentDocument = isParentDocument)
        }
    }
}

private inline fun <reified T : MarkdownNode> Node.convertChildren(): List<T> {
    val children = mutableListOf<T>()
    var child = this.firstChild
    while (child != null) {
        child.toContent(this.parent is Document).let {
            if (it is T) {
                children.add(it)
            }
        }
        child = child.next
    }
    return children
}

@Suppress("LongMethod")
fun MarkdownNode.filterNodesContainingQuery(query: String): MarkdownNode? {
    return when (this) {
        is MarkdownNode.Document,
        is MarkdownNode.Block.Heading,
        is MarkdownNode.Block.Paragraph,
        is MarkdownNode.Block.BlockQuote,
        is MarkdownNode.Block.ListBlock,
        is MarkdownNode.Block.ListItem,
        is MarkdownNode.Block.Table,
        is MarkdownNode.Block.TableContent.Head,
        is MarkdownNode.Block.TableContent.Body -> {
            val filteredChildren = children.mapNotNull { it.filterNodesContainingQuery(query) }
            if (filteredChildren.any { it.containsQuery(query) }) {
                this.copy(children = filteredChildren)
            } else {
                null
            }
        }

        is MarkdownNode.TableRow -> {
            val filteredChildren = children.mapNotNull { it.filterNodesContainingQuery(query) }
            if (filteredChildren.any { it.containsQuery(query) }) {
                this.copy(children = filteredChildren.filterIsInstance<MarkdownNode.TableCell>())
            } else {
                null
            }
        }

        is MarkdownNode.Block.IntendedCode -> processLiteral(literal, query)?.let { this.copy(literal = it) }
        is MarkdownNode.Block.FencedCode -> processLiteral(literal, query)?.let { this.copy(literal = it) }
        is MarkdownNode.Inline.Text -> processLiteral(literal, query)?.let { this.copy(literal = it) }
        is MarkdownNode.Inline.Code -> processLiteral(literal, query)?.let { this.copy(literal = it) }

        is MarkdownNode.Inline.Link,
        is MarkdownNode.Inline.StrongEmphasis,
        is MarkdownNode.Inline.Emphasis,
        is MarkdownNode.Inline.Strikethrough -> {
            val filteredChildren = children.mapNotNull { it.filterNodesContainingQuery(query) }
            if (filteredChildren.any { it.containsQuery(query) }) {
                this.copy(children = filteredChildren.filterIsInstance<MarkdownNode.Inline>())
            } else {
                null
            }
        }

        is MarkdownNode.TableCell -> {
            val filteredChildren = children.mapNotNull { it.filterNodesContainingQuery(query) }
            if (filteredChildren.any { it.containsQuery(query) }) {
                this.copy(children = filteredChildren.filterIsInstance<MarkdownNode.Inline>())
            } else {
                null
            }
        }

        is MarkdownNode.Inline.Image -> if (title?.contains(query, ignoreCase = true) == true
            || destination.contains(query, ignoreCase = true)
        ) {
            this
        } else {
            null
        }

        is MarkdownNode.Block.ThematicBreak -> null
        is MarkdownNode.Inline.Break -> this
        is MarkdownNode.Unsupported -> null
    }
}

fun MarkdownNode.getFirstInlines(): MarkdownPreview? {
    return when (this) {
        is MarkdownNode.Document -> children.firstOrNull()?.getFirstInlines()
        is MarkdownNode.Block.BlockQuote -> children.firstOrNull()?.getFirstInlines()
        is MarkdownNode.Block.FencedCode -> literal.toPreview()
        is MarkdownNode.Block.Heading -> children.toPreview()
        is MarkdownNode.Block.IntendedCode -> literal.toPreview()
        is MarkdownNode.Block.ListBlock.Bullet -> children.firstOrNull()?.getFirstInlines()
        is MarkdownNode.Block.ListBlock.Ordered -> children.firstOrNull()?.getFirstInlines()
        is MarkdownNode.Block.ListItem -> children.firstOrNull()?.getFirstInlines()
        is MarkdownNode.Block.Paragraph -> children.toPreview()
        is MarkdownNode.Block.Table -> children.firstOrNull()?.getFirstInlines()
        is MarkdownNode.Block.TableContent.Body -> children.firstOrNull()?.getFirstInlines()
        is MarkdownNode.Block.TableContent.Head -> children.firstOrNull()?.getFirstInlines()
        is MarkdownNode.Block.ThematicBreak -> null
        is MarkdownNode.Inline -> {
            throw IllegalArgumentException("It should not go to inline children!")
        }

        is MarkdownNode.TableCell -> children.toPreview()
        is MarkdownNode.TableRow -> children.firstOrNull()?.children?.toPreview()
        is MarkdownNode.Unsupported -> null
    }
}

private fun List<MarkdownNode>.isNotBlank(): Boolean = this.any {
    when (it) {
        is MarkdownNode.Document -> it.children.isNotBlank()
        is MarkdownNode.Block.BlockQuote -> it.children.isNotBlank()
        is MarkdownNode.Block.FencedCode -> it.literal.isNotBlank()
        is MarkdownNode.Block.Heading -> it.children.isNotBlank()
        is MarkdownNode.Block.IntendedCode -> it.literal.isNotBlank()
        is MarkdownNode.Block.ListBlock.Bullet -> it.children.isNotBlank()
        is MarkdownNode.Block.ListBlock.Ordered -> it.children.isNotBlank()
        is MarkdownNode.Block.ListItem -> it.children.isNotBlank()
        is MarkdownNode.Block.Paragraph -> it.children.isNotBlank()
        is MarkdownNode.Block.Table -> it.children.isNotBlank()
        is MarkdownNode.Block.TableContent.Body -> it.children.isNotBlank()
        is MarkdownNode.Block.TableContent.Head -> it.children.isNotBlank()
        is MarkdownNode.Block.ThematicBreak -> true
        is MarkdownNode.Inline.Break -> true
        is MarkdownNode.Inline.Code -> it.literal.isNotBlank()
        is MarkdownNode.Inline.Emphasis -> it.children.isNotBlank()
        is MarkdownNode.Inline.Image -> it.destination.isNotBlank()
        is MarkdownNode.Inline.Link -> it.destination.isNotBlank()
        is MarkdownNode.Inline.Strikethrough -> it.children.isNotBlank()
        is MarkdownNode.Inline.StrongEmphasis -> it.children.isNotBlank()
        is MarkdownNode.Inline.Text -> it.literal.isNotBlank()
        is MarkdownNode.TableCell -> it.children.isNotBlank()
        is MarkdownNode.TableRow -> it.children.isNotBlank()
        is MarkdownNode.Unsupported -> false
    }
}

fun MarkdownNode.Document.isNotBlank(): Boolean = children.isNotBlank()

private fun List<MarkdownNode.Inline>.toPreview(): MarkdownPreview {
    return MarkdownPreview(this.toPersistentList())
}

private fun String.toPreview(): MarkdownPreview {
    return MarkdownPreview(persistentListOf(MarkdownNode.Inline.Text(this)))
}

private fun MarkdownNode.containsQuery(query: String): Boolean {
    return when (this) {
        is MarkdownNode.Inline.Text -> literal.contains(query, ignoreCase = true)
        is MarkdownNode.Block.FencedCode -> literal.contains(query, ignoreCase = true)
        is MarkdownNode.Block.IntendedCode -> literal.contains(query, ignoreCase = true)
        is MarkdownNode.Inline.Code -> literal.contains(query, ignoreCase = true)
        is MarkdownNode.Inline.Link -> title?.contains(query, ignoreCase = true) == true || children.anyContainsQuery(query)
        is MarkdownNode.Inline.Image -> title?.contains(query, ignoreCase = true) == true || children.anyContainsQuery(query)
        else -> children.anyContainsQuery(query)
    }
}

private fun processLiteral(inputString: String, queryString: String, maxWordsAround: Int = 3): String? {
    val matches = queryString.toRegex(option = RegexOption.IGNORE_CASE).findAll(inputString).toList()
    if (matches.isEmpty()) return null

    val result = StringBuilder()
    var lastEnd = 0

    matches.forEachIndexed { index, matchResult ->
        val matchStart = matchResult.range.first
        val matchEnd = matchResult.range.last + 1

        val contextStart = maxOf(findContextStart(inputString, matchStart, maxWordsAround), lastEnd)
        val contextEnd = findContextEnd(inputString, matchEnd, maxWordsAround)

        if (index == 0 && contextStart > 0) {
            result.append("...")
        }

        if (index != 0 && contextStart - lastEnd > 1) {
            result.append("...")
        }

        if (contextStart < contextEnd) {
            if (lastEnd != 0 && contextStart <= lastEnd) {
                result.append(inputString.substring(lastEnd, contextEnd))
            } else {
                result.append(inputString.substring(contextStart, contextEnd))
            }
        }

        lastEnd = contextEnd
    }

    if (lastEnd < inputString.length) {
        result.append("...")
    }

    return if (result.isEmpty()) null else result.toString()
}

fun findContextStart(inputString: String, matchStart: Int, wordsAround: Int): Int {
    var spaceCount = 0
    var index = matchStart - 1
    while (index >= 0 && spaceCount < wordsAround) {
        if (inputString[index].isWhitespace()) spaceCount++
        index--
    }
    return index + 1
}

fun findContextEnd(inputString: String, matchEnd: Int, wordsAround: Int): Int {
    var spaceCount = 0
    var index = matchEnd
    while (index < inputString.length && spaceCount < wordsAround) {
        if (inputString[index].isWhitespace()) spaceCount++
        index++
    }
    return index
}

private fun List<MarkdownNode>.anyContainsQuery(query: String): Boolean {
    return any { it.containsQuery(query) }
}

private fun MarkdownNode.copy(children: List<MarkdownNode>): MarkdownNode {
    return when (this) {
        is MarkdownNode.Document -> this.copy(children = children.filterIsInstance<MarkdownNode.Block>())
        // Block nodes
        is MarkdownNode.Block.Heading -> this.copy(children = children.filterIsInstance<MarkdownNode.Inline>())
        is MarkdownNode.Block.Paragraph -> this.copy(children = children.filterIsInstance<MarkdownNode.Inline>())
        is MarkdownNode.Block.BlockQuote -> this.copy(children = children.filterIsInstance<MarkdownNode.Block>())
        is MarkdownNode.Block.ListBlock.Bullet -> this.copy(children = children.filterIsInstance<MarkdownNode.Block.ListItem>())
        is MarkdownNode.Block.ListBlock.Ordered -> this.copy(children = children.filterIsInstance<MarkdownNode.Block.ListItem>())
        is MarkdownNode.Block.ListItem -> this.copy(children = children.filterIsInstance<MarkdownNode.Block>())
        is MarkdownNode.Block.IntendedCode -> this.copy(children = children.filterIsInstance<MarkdownNode.Inline>())
        is MarkdownNode.Block.FencedCode -> this.copy(children = children.filterIsInstance<MarkdownNode.Inline>())
        is MarkdownNode.Block.Table -> this.copy(children = children.filterIsInstance<MarkdownNode.Block.TableContent>())
        is MarkdownNode.Block.TableContent.Head -> this.copy(children = children.filterIsInstance<MarkdownNode.TableRow>())
        is MarkdownNode.Block.TableContent.Body -> this.copy(children = children.filterIsInstance<MarkdownNode.TableRow>())
        is MarkdownNode.Block.ThematicBreak -> this.copy(children = children.filterIsInstance<MarkdownNode.Inline>())

        // Inline Nodes
        is MarkdownNode.Inline.Text -> this
        is MarkdownNode.Inline.StrongEmphasis -> this.copy(children = children.filterIsInstance<MarkdownNode.Inline>())
        is MarkdownNode.Inline.Emphasis -> this.copy(children = children.filterIsInstance<MarkdownNode.Inline>())
        is MarkdownNode.Inline.Link -> this.copy(children = children.filterIsInstance<MarkdownNode.Inline>())
        is MarkdownNode.Inline.Image -> this.copy(children = children.filterIsInstance<MarkdownNode.Inline>())
        is MarkdownNode.Inline.Code -> this.copy(children = children.filterIsInstance<MarkdownNode.Inline>())
        is MarkdownNode.Inline.Strikethrough -> this.copy(children = children.filterIsInstance<MarkdownNode.Inline>())
        is MarkdownNode.Inline.Break -> this
        // Custom nodes
        is MarkdownNode.TableRow -> this.copy(children = children.filterIsInstance<MarkdownNode.TableCell>())
        is MarkdownNode.TableCell -> this.copy(children = children.filterIsInstance<MarkdownNode.Inline>())
        is MarkdownNode.Unsupported -> this
    }
}

// use it to investigate when some of the markdown messages are not properly showed
fun printMarkdownNodeTree(node: MarkdownNode?, indentLevel: Int = 0) {
    node ?: return

    val indent = " ".repeat(indentLevel * 2)
    val printLog = when (node) {
        is MarkdownNode.Document -> "${indent}Document: [${node.children.size} children]"
        is MarkdownNode.Block.Heading -> "${indent}Heading(level=${node.level}): [${node.children.size} children]"
        is MarkdownNode.Block.Paragraph -> "${indent}Paragraph: [${node.children.size} children]"
        is MarkdownNode.Block.BlockQuote -> "${indent}BlockQuote: [${node.children.size} children]"
        is MarkdownNode.Block.ListBlock.Bullet ->
            "${indent}BulletList(marker=${node.bulletMarker}): [${node.children.size} children]"

        is MarkdownNode.Block.ListBlock.Ordered ->
            "${indent}OrderedList(startNumber=${node.startNumber}, delimiter=${node.delimiter}): [${node.children.size} children]"

        is MarkdownNode.Block.ListItem -> "${indent}ListItem: [${node.children.size} children]"
        is MarkdownNode.Block.IntendedCode ->
            "${indent}IntendedCode: [${node.children.size} children] '${node.literal}'"

        is MarkdownNode.Block.FencedCode -> "${indent}FencedCode: [${node.children.size} children] '${node.literal}'"
        is MarkdownNode.Block.Table -> "${indent}Table: [${node.children.size} children]"
        is MarkdownNode.Block.TableContent.Head -> "${indent}TableHead: [${node.children.size} children]"
        is MarkdownNode.Block.TableContent.Body -> "${indent}TableBody: [${node.children.size} children]"
        is MarkdownNode.TableRow -> "${indent}TableRow: [${node.children.size} children]"
        is MarkdownNode.Block.ThematicBreak -> "${indent}ThematicBreak"

        is MarkdownNode.TableCell -> "${indent}TableCell: [${node.children.size} children]"
        is MarkdownNode.Inline.Text -> "${indent}Text: '${node.literal}'"
        is MarkdownNode.Inline.Link ->
            "${indent}Link(destination='${node.destination}', title='${node.title}'): [${node.children.size} children]"

        is MarkdownNode.Inline.Image -> "${indent}Image(destination='${node.destination}', title='${node.title}')"
        is MarkdownNode.Inline.StrongEmphasis -> "${indent}StrongEmphasis: [${node.children.size} children]"
        is MarkdownNode.Inline.Emphasis -> "${indent}Emphasis: [${node.children.size} children]"
        is MarkdownNode.Inline.Code -> "${indent}Code: '${node.literal}'"
        is MarkdownNode.Inline.Strikethrough -> "${indent}Strikethrough: [${node.children.size} children]"
        is MarkdownNode.Inline.Break -> "${indent}Break"
        is MarkdownNode.Unsupported -> "${indent}Unsupported"
    }
    println(printLog)

    node.children.forEach { printMarkdownNodeTree(it, indentLevel + 1) }
}
