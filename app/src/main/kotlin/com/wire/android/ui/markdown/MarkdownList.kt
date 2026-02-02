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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.messages.item.onBackground
import com.wire.android.ui.markdown.MarkdownConstants.BULLET_MARK
import com.wire.android.ui.theme.wireTypography

@Composable
fun MarkdownBulletList(bulletList: MarkdownNode.Block.ListBlock.Bullet, nodeData: NodeData, modifier: Modifier = Modifier) {
    val bottom = if (bulletList.isParentDocument) dimensions().spacing8x else dimensions().spacing0x

    val text = buildAnnotatedString {
        pushStyle(MaterialTheme.wireTypography.body01.copy(color = nodeData.messageStyle.onBackground()).toSpanStyle())
        append("$BULLET_MARK ")
        pop()
    }

    Column(modifier = modifier.padding(bottom = bottom)) {
        bulletList.children.forEach { listItem ->
            Row {
                MarkdownText(
                    annotatedString = text,
                    style = MaterialTheme.wireTypography.body01.copy(color = nodeData.messageStyle.onBackground()),
                    onLongClick = nodeData.actions?.onLongClick,
                    onOpenProfile = nodeData.actions?.onOpenProfile
                )
                MarkdownNodeBlockChildren(children = listItem.children, nodeData = nodeData)
            }
        }
    }
}

@Composable
fun MarkdownOrderedList(
    orderedList: MarkdownNode.Block.ListBlock.Ordered,
    nodeData: NodeData,
    modifier: Modifier = Modifier
) {
    val bottom = if (orderedList.isParentDocument) dimensions().spacing8x else dimensions().spacing0x

    Column(modifier = modifier.padding(bottom = bottom)) {
        orderedList.children.forEach { listItem ->
            val text = buildAnnotatedString {
                pushStyle(MaterialTheme.wireTypography.body01.copy(color = nodeData.messageStyle.onBackground()).toSpanStyle())
                append("${listItem.orderNumber}${orderedList.delimiter} ")
                pop()
            }

            Row {
                MarkdownText(
                    annotatedString = text,
                    style = MaterialTheme.wireTypography.body01.copy(color = nodeData.messageStyle.onBackground()),
                    onLongClick = nodeData.actions?.onLongClick,
                    onOpenProfile = nodeData.actions?.onOpenProfile
                )
                MarkdownNodeBlockChildren(children = listItem.children, nodeData = nodeData)
            }
        }
    }
}
