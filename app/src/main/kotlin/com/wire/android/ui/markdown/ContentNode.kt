/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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

import org.commonmark.node.*

sealed class ContentNode {
    abstract val children: List<ContentNode>

    sealed class Block : ContentNode() {
        class Document(override val children: List<ContentNode> = listOf()) : Block()
        class Heading(val level: Int, override val children: List<ContentNode> = listOf()) : Block()
        class Paragraph(override val children: List<ContentNode> = listOf()) : Block()
        class BlockQuote(override val children: List<ContentNode> = listOf()) : Block()
        class ListBlock(override val children: List<ContentNode>) : Block()
        class ListItem(override val children: List<ContentNode> = listOf()) : Block()
    }

    sealed class Inline : ContentNode() {
        class Text(val literal: String) : Inline() {
            override val children: List<ContentNode> = listOf()
        }
        class StrongEmphasis(override val children: List<ContentNode> = listOf()) : Inline()
        class Emphasis(override val children: List<ContentNode> = listOf()) : Inline()
        class Link(val destination: String, val title: String, override val children: List<ContentNode> = listOf()) : Inline()
        class Image(val destination: String, val title: String, override val children: List<ContentNode> = listOf()) : Inline()
        class Code(val literal: String) : Inline() {
            override val children: List<ContentNode> = listOf()
        }
    }

    class HtmlInline(override val children: List<ContentNode> = listOf()) : Inline()
    class HtmlBlock(override val children: List<ContentNode> = listOf()) : Block()
    class ThematicBreak(override val children: List<ContentNode> = listOf()) : Block()
}

@Suppress("ComplexMethod")
fun Node.toContent(): ContentNode {
    return when (this) {
        is Document -> ContentNode.Block.Document(convertChildren(this))
        is Heading -> ContentNode.Block.Heading(this.level, convertChildren(this))
        is Paragraph -> ContentNode.Block.Paragraph(convertChildren(this))
        is BlockQuote -> ContentNode.Block.BlockQuote(convertChildren(this))
        is ListBlock -> ContentNode.Block.ListBlock(convertChildren(this))
        is ListItem -> ContentNode.Block.ListItem(convertChildren(this))
        is Text -> ContentNode.Inline.Text(this.literal)
        is StrongEmphasis -> ContentNode.Inline.StrongEmphasis(convertChildren(this))
        is Emphasis -> ContentNode.Inline.Emphasis(convertChildren(this))
        is Link -> ContentNode.Inline.Link(this.destination, this.title, convertChildren(this))
        is Image -> ContentNode.Inline.Image(this.destination, this.title, convertChildren(this))
        is Code -> ContentNode.Inline.Code(this.literal)
        is HtmlInline -> ContentNode.HtmlInline(convertChildren(this))
        is HtmlBlock -> ContentNode.HtmlBlock(convertChildren(this))
        is ThematicBreak -> ContentNode.ThematicBreak(convertChildren(this))
        else -> throw IllegalArgumentException("Unsupported node type: ${this.javaClass.simpleName}")
    }
}

private fun convertChildren(node: Node): List<ContentNode> {
    val children = mutableListOf<ContentNode>()
    var child = node.firstChild
    while (child != null) {
        children.add(child.toContent())
        child = child.next
    }
    return children
}
