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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.messages.item.onBackground

@Composable
@Suppress("MagicNumber")
fun MarkdownHeading(heading: MarkdownNode.Block.Heading, nodeData: NodeData, modifier: Modifier = Modifier) {
    val style: TextStyle = when (heading.level) {
        1 -> nodeData.typography.title01
        2 -> nodeData.typography.title01
        3 -> nodeData.typography.title01
        4 -> nodeData.typography.title01
        5 -> nodeData.typography.title01
        6 -> nodeData.typography.title01
        else -> nodeData.typography.title01
    }

    val padding = if (heading.isParentDocument) dimensions().spacing8x else dimensions().spacing0x

    Box(modifier = modifier.padding(bottom = padding)) {
        val text = buildAnnotatedString {
            inlineNodeChildren(heading.children, this, nodeData)
        }
        MarkdownText(
            annotatedString = text,
            style = style.copy(color = nodeData.messageStyle.onBackground()),
            onLongClick = nodeData.actions?.onLongClick,
            onOpenProfile = nodeData.actions?.onOpenProfile
        )
    }
}
