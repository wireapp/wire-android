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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun MarkdownParagraph(
    paragraph: MarkdownNode.Block.Paragraph,
    nodeData: NodeData,
    clickable: Boolean,
    onMentionsUpdate: (List<DisplayMention>) -> Unit,
    modifier: Modifier = Modifier
) {
    val padding = if (paragraph.isParentDocument) dimensions().spacing4x else dimensions().spacing0x
    Box(modifier = modifier.padding(bottom = padding)) {
        val annotatedString = buildAnnotatedString {
            pushStyle(MaterialTheme.wireTypography.body01.toSpanStyle().copy(color = nodeData.color))
            val updatedMentions = inlineNodeChildren(paragraph.children, this, nodeData)
            onMentionsUpdate(updatedMentions)
            pop()
        }
        MarkdownText(
            annotatedString,
            style = nodeData.style,
            onLongClick = nodeData.actions?.onLongClick,
            onOpenProfile = nodeData.actions?.onOpenProfile,
            onClickLink = nodeData.actions?.onLinkClick,
            clickable = clickable
        )
    }
}
