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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.markdown.MarkdownConstants.BULLET_MARK
import com.wire.android.ui.theme.wireTypography
import org.commonmark.node.BulletList
import org.commonmark.node.Document
import org.commonmark.node.ListBlock
import org.commonmark.node.Node
import org.commonmark.node.OrderedList

// TODO remove
@Composable
fun MarkdownBulletList(bulletList: BulletList, nodeData: NodeData) {
    MarkdownListItems(bulletList, nodeData) {
        val text = buildAnnotatedString {
            pushStyle(MaterialTheme.wireTypography.body01.toSpanStyle())
            append("$BULLET_MARK ")
            inlineChildren(it, this, nodeData)
            pop()
        }
        MarkdownText(
            annotatedString = text,
            style = MaterialTheme.wireTypography.body01,
            onLongClick = nodeData.onLongClick,
            onOpenProfile = nodeData.onOpenProfile
        )
    }
}

@Composable
fun MarkdownOrderedList(orderedList: OrderedList, nodeData: NodeData) {
    var number = orderedList.startNumber
    val delimiter = orderedList.delimiter
    MarkdownListItems(orderedList, nodeData) {
        val text = buildAnnotatedString {
            pushStyle(MaterialTheme.wireTypography.body01.toSpanStyle())
            append("${number++}$delimiter ")
            inlineChildren(it, this, nodeData)
            pop()
        }
        MarkdownText(
            annotatedString = text,
            style = MaterialTheme.wireTypography.body01,
            onLongClick = nodeData.onLongClick,
            onOpenProfile = nodeData.onOpenProfile
        )
    }
}

@Composable
fun MarkdownListItems(listBlock: ListBlock, nodeData: NodeData, item: @Composable (node: Node) -> Unit) {
    val bottom = if (listBlock.parent is Document) dimensions().spacing8x else dimensions().spacing0x
    val start = if (listBlock.parent is Document) dimensions().spacing0x else dimensions().spacing8x
    Column(modifier = Modifier.padding(start = start, bottom = bottom)) {
        var listItem = listBlock.firstChild
        while (listItem != null) {
            var child = listItem.firstChild
            while (child != null) {
                when (child) {
                    is BulletList -> MarkdownBulletList(child, nodeData)
                    is OrderedList -> MarkdownOrderedList(child, nodeData)
                    else -> item(child)
                }
                child = child.next
            }
            listItem = listItem.next
        }
    }
}

@Composable
fun MarkdownNodeBulletList(bulletList: MarkdownNode.Block.ListBlock.Bullet, nodeData: NodeData) {
    val bottom = if (bulletList.isParentDocument) dimensions().spacing8x else dimensions().spacing0x
    val start = if (bulletList.isParentDocument) dimensions().spacing0x else dimensions().spacing8x

    val text = buildAnnotatedString {
        pushStyle(MaterialTheme.wireTypography.body01.toSpanStyle())
        append("$BULLET_MARK ")
        pop()
    }

    Column(modifier = Modifier.padding(bottom = bottom)) {
        bulletList.children.forEach { listItem ->
            Row {
                MarkdownText(
                    annotatedString = text,
                    style = MaterialTheme.wireTypography.body01,
                    onLongClick = nodeData.onLongClick,
                    onOpenProfile = nodeData.onOpenProfile
                )
                MarkdownNodeBlockChildren(children = listItem.children, nodeData = nodeData)
            }
        }
    }
}

@Composable
fun MarkdownNodeOrderedList(orderedList: MarkdownNode.Block.ListBlock.Ordered, nodeData: NodeData) {
    val bottom = if (orderedList.isParentDocument) dimensions().spacing8x else dimensions().spacing0x
    val start = if (orderedList.isParentDocument) dimensions().spacing0x else dimensions().spacing8x

    Column(modifier = Modifier.padding(bottom = bottom)) {
        orderedList.children.forEach { listItem ->
            val text = buildAnnotatedString {
                pushStyle(MaterialTheme.wireTypography.body01.toSpanStyle())
                append("${listItem.orderNumber}${orderedList.delimiter} ")
                pop()
            }

            Row {
                MarkdownText(
                    annotatedString = text,
                    style = MaterialTheme.wireTypography.body01,
                    onLongClick = nodeData.onLongClick,
                    onOpenProfile = nodeData.onOpenProfile
                )
                MarkdownNodeBlockChildren(children = listItem.children, nodeData = nodeData)
            }
        }
    }
}
