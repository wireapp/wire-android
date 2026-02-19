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

package com.wire.android.ui.home.conversationslist.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.ui.markdown.MarkdownConstants
import com.wire.android.ui.markdown.MarkdownInline
import com.wire.android.ui.markdown.MarkdownPreview
import com.wire.android.ui.markdown.MarkdownNode
import com.wire.android.ui.markdown.MessageColors
import com.wire.android.ui.markdown.NodeData
import com.wire.android.ui.theme.Accent
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.UIText
import kotlinx.collections.immutable.persistentListOf

@Composable
fun LastMessageSubtitle(text: UIText, markdownPreview: MarkdownPreview? = null, markdownLocaleTag: String? = null) {
    LastMessageMarkdown(text = text.asString(), markdownPreview = markdownPreview, markdownLocaleTag = markdownLocaleTag)
}

@Composable
fun LastMessageSubtitleWithAuthor(
    author: UIText,
    text: UIText,
    separator: String,
    markdownPreview: MarkdownPreview? = null,
    markdownLocaleTag: String? = null
) {
    LastMessageMarkdown(
        text = text.asString(),
        leadingText = "${author.asString()}$separator",
        markdownPreview = markdownPreview,
        markdownLocaleTag = markdownLocaleTag
    )
}

@Composable
fun LastMultipleMessages(messages: List<UIText>, separator: String) {
    LastMessageMarkdown(text = messages.map { it.asString() }.joinToString(separator = separator))
}

@Composable
private fun LastMessageMarkdown(
    text: String,
    leadingText: String = "",
    markdownPreview: MarkdownPreview? = null,
    markdownLocaleTag: String? = null
) {
    val nodeData = NodeData(
        color = MaterialTheme.wireColorScheme.secondaryText,
        style = MaterialTheme.wireTypography.subline01,
        colorScheme = MaterialTheme.wireColorScheme,
        typography = MaterialTheme.wireTypography,
        searchQuery = "",
        mentions = listOf(),
        disableLinks = true,
        messageColors = MessageColors(highlighted = MaterialTheme.wireColorScheme.primary),
        accent = Accent.Unknown
    )

    val locales = LocalConfiguration.current.locales
    val currentLocaleTag = if (locales.isEmpty) "" else locales[0].toLanguageTag()
    val shouldUsePreview = markdownPreview != null && (markdownLocaleTag == null || markdownLocaleTag == currentLocaleTag)

    if (shouldUsePreview) {
        val leadingInlines = if (leadingText.isBlank()) {
            persistentListOf()
        } else {
            persistentListOf(
                MarkdownNode.Inline.Text(
                    leadingText.replace(MarkdownConstants.NON_BREAKING_SPACE, " ")
                )
            )
        }
        MarkdownInline(
            inlines = leadingInlines.plus(markdownPreview.children),
            nodeData = nodeData
        )
    } else {
        Text(
            text = leadingText.replace(MarkdownConstants.NON_BREAKING_SPACE, " ") +
                text.replace(MarkdownConstants.NON_BREAKING_SPACE, " "),
            style = nodeData.style,
            color = nodeData.color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
