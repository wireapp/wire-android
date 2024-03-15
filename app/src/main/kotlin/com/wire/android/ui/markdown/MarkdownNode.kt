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

sealed class MarkdownNode {
    abstract val children: List<MarkdownNode>
    abstract val isParentDocument: Boolean

    data class Document(
        override val children: List<Block> = listOf(),
        override val isParentDocument: Boolean = false
    ) : MarkdownNode()

    sealed class Block : MarkdownNode() {
        data class Heading(
            override val children: List<Inline> = listOf(),
            override val isParentDocument: Boolean,
            val level: Int
        ) : Block()

        data class Paragraph(
            override val children: List<Inline> = listOf(),
            override val isParentDocument: Boolean
        ) : Block()

        data class BlockQuote(
            override val children: List<Block> = listOf(),
            override val isParentDocument: Boolean
        ) : Block()

        sealed class ListBlock(override val children: List<ListItem>) : Block() {
            data class Bullet(
                override val children: List<ListItem>,
                override val isParentDocument: Boolean = false,
                val bulletMarker: Char
            ) : ListBlock(children)

            data class Ordered(
                override val children: List<ListItem>,
                override val isParentDocument: Boolean = false,
                val startNumber: Int,
                val delimiter: Char,
            ) : ListBlock(children)
        }

        data class ListItem(
            override val children: List<Block> = listOf(),
            override val isParentDocument: Boolean = false,
            val orderNumber: Int
        ) : Block()

        data class IntendedCode(
            override val isParentDocument: Boolean,
            val literal: String
        ) : Block() {
            override val children: List<MarkdownNode>
                get() = listOf()
        }

        data class FencedCode(
            override val isParentDocument: Boolean,
            val literal: String
        ) : Block() {
            override val children: List<MarkdownNode>
                get() = listOf()
        }

        data class Table(
            override val children: List<TableContent> = listOf(),
            override val isParentDocument: Boolean
        ) : Block()

        sealed class TableContent : Block() {
            data class Head(
                override val children: List<TableRow>,
                override val isParentDocument: Boolean = false
            ) : TableContent()

            data class Body(
                override val children: List<TableRow>,
                override val isParentDocument: Boolean = false
            ) : TableContent()
        }

        data class ThematicBreak(
            override val children: List<Inline> = listOf(),
            override val isParentDocument: Boolean
        ) : Block()
    }

    sealed class Inline(override val isParentDocument: Boolean = false) : MarkdownNode() {
        abstract override val children: List<Inline>

        data class Text(val literal: String) : Inline() {
            override val children: List<Inline> = listOf()
        }

        data class StrongEmphasis(override val children: List<Inline> = listOf()) : Inline()
        data class Strikethrough(override val children: List<Inline> = listOf()) : Inline()
        data class Emphasis(override val children: List<Inline> = listOf()) : Inline()
        data class Link(
            val destination: String,
            val title: String?,
            override val children: List<Inline> = listOf()
        ) : Inline()

        data class Image(
            val destination: String,
            val title: String?,
            override val children: List<Inline> = listOf()
        ) : Inline()

        data class Code(val literal: String) : Inline() {
            override val children: List<Inline> = listOf()
        }

        data class Break(override val children: List<Inline> = listOf()) : Inline()
    }

    data class TableRow(
        override val children: List<TableCell>,
        override val isParentDocument: Boolean = false
    ) : MarkdownNode()

    data class TableCell(
        override val children: List<Inline> = listOf(),
        override val isParentDocument: Boolean = false
    ) : MarkdownNode()
}
