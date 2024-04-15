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

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun MarkdownInline(
    inlines: List<MarkdownNode.Inline>,
    nodeData: NodeData
) {
    val annotatedString = buildAnnotatedString {
        pushStyle(nodeData.style.toSpanStyle())
        inlineNodeChildren(inlines, this, nodeData)
        pop()
    }
    MarkdownText(
        annotatedString,
        style = nodeData.style,
        color = nodeData.color,
        clickable = false,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
