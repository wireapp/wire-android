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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.messages.item.onBackground
import com.wire.android.ui.home.conversations.messages.item.textColor
import com.wire.android.ui.theme.wireTypography

@Composable
fun MarkdownBlockQuote(blockQuote: MarkdownNode.Block.BlockQuote, nodeData: NodeData, modifier: Modifier = Modifier) {
    val color = nodeData.messageStyle.textColor()
    val xOffset = dimensions().spacing12x.value
    Column(
        modifier = modifier
            .drawBehind {
                drawLine(
                    color = color,
                    strokeWidth = 2f,
                    start = Offset(xOffset, 0f),
                    end = Offset(xOffset, size.height)
                )
            }
            .padding(
                start = dimensions().spacing16x,
                top = dimensions().spacing4x,
                bottom = dimensions().spacing4x
            )
    ) {

        blockQuote.children.map { child ->
            when (child) {
                is MarkdownNode.Block.BlockQuote -> MarkdownBlockQuote(child, nodeData)
                is MarkdownNode.Block.Paragraph -> {
                    val text = buildAnnotatedString {
                        pushStyle(
                            MaterialTheme.wireTypography.body01.copy(color = nodeData.messageStyle.onBackground()).toSpanStyle()
                                .plus(SpanStyle(fontStyle = FontStyle.Italic))
                        )
                        inlineNodeChildren(child.children, this, nodeData)
                        pop()
                    }
                    MarkdownText(
                        text,
                        onLongClick = nodeData.actions?.onLongClick,
                        onOpenProfile = nodeData.actions?.onOpenProfile
                    )
                }

                else -> MarkdownNodeBlockChildren(
                    children = child.children.filterIsInstance<MarkdownNode.Block>(),
                    nodeData = nodeData
                )
            }
        }
    }
}
