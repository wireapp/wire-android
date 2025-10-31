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

import android.text.util.Linkify
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.wire.android.ui.common.LinkSpannableString
import com.wire.android.ui.home.conversations.messages.item.MessageStyle
import com.wire.android.ui.markdown.MarkdownConstants.TAG_URL
import com.wire.android.util.MatchQueryResult
import com.wire.android.util.QueryMatchExtractor
import com.wire.kalium.logic.data.message.mention.MessageMention
import kotlin.math.max
import kotlin.math.min

@Composable
fun MarkdownDocument(
    document: MarkdownNode.Document,
    nodeData: NodeData,
    clickable: Boolean
) {
    val filteredDocument = if (nodeData.searchQuery.isNotBlank()) {
        document.filterNodesContainingQuery(nodeData.searchQuery)
    } else {
        document
    }
    if (filteredDocument != null) {
        MarkdownNodeBlockChildren(
            children = (filteredDocument as MarkdownNode.Document).children,
            nodeData = nodeData,
            clickable = clickable
        )
    }
}

@Composable
fun MarkdownNodeBlockChildren(
    children: List<MarkdownNode.Block>,
    nodeData: NodeData,
    modifier: Modifier = Modifier,
    clickable: Boolean = true
) {
    var updateMentions = nodeData.mentions
    val updatedNodeData = nodeData.copy(mentions = updateMentions)

    Column(modifier = modifier) {
        children.map { node ->
            when (node) {
                is MarkdownNode.Block.BlockQuote -> MarkdownBlockQuote(node, updatedNodeData)
                is MarkdownNode.Block.IntendedCode -> MarkdownIndentedCodeBlock(indentedCodeBlock = node, nodeData = updatedNodeData)
                is MarkdownNode.Block.FencedCode -> MarkdownFencedCodeBlock(fencedCodeBlock = node, nodeData = updatedNodeData)
                is MarkdownNode.Block.Heading -> MarkdownHeading(heading = node, nodeData = updatedNodeData)
                is MarkdownNode.Block.ListBlock.Ordered -> MarkdownOrderedList(orderedList = node, nodeData = updatedNodeData)
                is MarkdownNode.Block.ListBlock.Bullet -> MarkdownBulletList(bulletList = node, nodeData = updatedNodeData)

                is MarkdownNode.Block.Paragraph -> MarkdownParagraph(
                    paragraph = node,
                    nodeData = updatedNodeData,
                    clickable = clickable,
                    onMentionsUpdate = {
                        updateMentions = it
                    }
                )

                is MarkdownNode.Block.Table -> MarkdownTable(
                    tableBlock = node,
                    nodeData = updatedNodeData,
                    onMentionsUpdate = {
                        updateMentions = it
                    }
                )

                is MarkdownNode.Block.ThematicBreak -> MarkdownThematicBreak()

                // Not used Blocks here
                is MarkdownNode.Block.TableContent.Body -> {}
                is MarkdownNode.Block.ListItem -> {}
                is MarkdownNode.Block.TableContent.Head -> {}
            }
        }
    }
}

@Suppress("LongMethod")
fun inlineNodeChildren(
    children: List<MarkdownNode.Inline>,
    annotatedString: AnnotatedString.Builder,
    nodeData: NodeData
): List<DisplayMention> {

    var updatedMentions = nodeData.mentions

    children.forEach { child ->
        when (child) {
            is MarkdownNode.Inline.Text -> {
                if (nodeData.disableLinks) {
                    annotatedString.append(convertTypoGraphs(child.literal))
                } else {
                    updatedMentions = appendLinksAndMentions(
                        annotatedString,
                        convertTypoGraphs(child.literal),
                        nodeData.copy(mentions = updatedMentions)
                    )
                }
            }

            is MarkdownNode.Inline.Image -> {
                if (nodeData.disableLinks) {
                    annotatedString.append(child.destination)
                } else {
                    updatedMentions = appendLinksAndMentions(
                        annotatedString,
                        child.destination,
                        nodeData.copy(mentions = updatedMentions)
                    )
                }
            }

            is MarkdownNode.Inline.Emphasis -> {
                annotatedString.pushStyle(
                    SpanStyle(
                        fontFamily = nodeData.typography.body05.fontFamily,
                        fontStyle = FontStyle.Italic
                    )
                )
                updatedMentions = inlineNodeChildren(
                    child.children,
                    annotatedString,
                    nodeData
                )
                annotatedString.pop()
            }

            is MarkdownNode.Inline.StrongEmphasis -> {
                annotatedString.pushStyle(
                    SpanStyle(
                        fontFamily = nodeData.typography.body02.fontFamily,
                        fontWeight = FontWeight.Bold
                    )
                )
                updatedMentions = inlineNodeChildren(
                    child.children,
                    annotatedString,
                    nodeData
                )
                annotatedString.pop()
            }

            is MarkdownNode.Inline.Code -> {
                annotatedString.pushStyle(TextStyle(fontFamily = FontFamily.Monospace).toSpanStyle())
                annotatedString.append(highlightText(nodeData, child.literal))
                annotatedString.pop()
            }

            is MarkdownNode.Inline.Link -> {
                if (nodeData.disableLinks) {
                    annotatedString.append(child.destination)
                } else {
                    annotatedString.pushStyle(
                        SpanStyle(
                            color = nodeData.messageColors.highlighted,
                            textDecoration = TextDecoration.Underline
                        )
                    )
                    annotatedString.pushStringAnnotation(TAG_URL, child.destination)
                    updatedMentions = inlineNodeChildren(
                        child.children,
                        annotatedString,
                        nodeData
                    )
                    annotatedString.pop()
                    annotatedString.pop()
                }
            }

            is MarkdownNode.Inline.Strikethrough -> {
                annotatedString.pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                updatedMentions = inlineNodeChildren(child.children, annotatedString, nodeData)
                annotatedString.pop()
            }

            is MarkdownNode.Inline.Break -> annotatedString.append("\n")
        }
    }

    return updatedMentions
}

@Suppress("LongMethod", "ComplexMethod")
fun appendLinksAndMentions(
    annotatedString: AnnotatedString.Builder,
    string: String,
    nodeData: NodeData
): List<DisplayMention> {

    val stringBuilder = StringBuilder(string)
    val updatedMentions = nodeData.mentions.toMutableList()
    var highlightIndexes = emptyList<MatchQueryResult>()

    // get mentions from text, remove mention marks and update position of mentions
    val mentionList: List<MessageMention> = if (stringBuilder.contains(MarkdownConstants.MENTION_MARK) && updatedMentions.isNotEmpty()) {
        nodeData.mentions.mapNotNull { displayMention ->
            val markedMentionLength = (
                    MarkdownConstants.MENTION_MARK
                            + displayMention.mentionUserName
                            + MarkdownConstants.MENTION_MARK
                    ).length
            val startIndex = stringBuilder.indexOf(
                MarkdownConstants.MENTION_MARK
                        + displayMention.mentionUserName
                        + MarkdownConstants.MENTION_MARK
            )
            val endIndex = startIndex + markedMentionLength

            if (startIndex != -1) {
                stringBuilder.replace(startIndex, endIndex, displayMention.mentionUserName)
                // remove mention from list to not use the same mention twice
                updatedMentions.removeAt(0)
                MessageMention(
                    startIndex,
                    displayMention.length,
                    displayMention.userId,
                    displayMention.isSelfMention
                )
            } else {
                null
            }
        }
    } else {
        listOf()
    }

    if (nodeData.searchQuery.isNotBlank()) {
        highlightIndexes = QueryMatchExtractor.extractQueryMatchIndexes(
            matchText = nodeData.searchQuery,
            text = stringBuilder.toString()
        )
    }

    val linkInfos = LinkSpannableString.getLinkInfos(stringBuilder.toString(), Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES)

    val linkAndMentionColor = when (nodeData.messageStyle) {
        MessageStyle.BUBBLE_SELF -> if (nodeData.isAccentBackground) {
            nodeData.colorScheme.onScrim
        } else {
            nodeData.colorScheme.onPrimary
        }

        MessageStyle.BUBBLE_OTHER -> nodeData.colorScheme.primary
        MessageStyle.NORMAL -> nodeData.colorScheme.primary
    }

    val updatedAnnotatedString = buildAnnotatedString {
        append(stringBuilder)
        with(nodeData.colorScheme) {
            linkInfos.forEach {
                val safeStart = max(it.start, 0)
                val safeEnd = min(it.end, length)
                if (safeStart > safeEnd) {
                    return@forEach
                }
                addStyle(
                    style = SpanStyle(
                        color = linkAndMentionColor,
                        textDecoration = TextDecoration.Underline
                    ),
                    start = safeStart,
                    end = safeEnd
                )
                addStringAnnotation(
                    tag = TAG_URL,
                    annotation = it.url,
                    start = safeStart,
                    end = safeEnd
                )
            }

            if (mentionList.isNotEmpty()) {
                mentionList.forEach { mention ->
                    if (mention.length <= 0 || mention.start >= length || mention.start + mention.length > length) {
                        return@forEach
                    }
                    addStyle(
                        style = SpanStyle(
                            fontWeight = nodeData.typography.body02.fontWeight,
                            color = linkAndMentionColor,
                            background = if (mention.isSelfMention) primaryVariant else Color.Unspecified
                        ),
                        start = mention.start,
                        end = mention.start + mention.length
                    )
                    addStringAnnotation(
                        tag = MarkdownConstants.TAG_MENTION,
                        annotation = mention.userId.toString(),
                        start = mention.start,
                        end = mention.start + mention.length
                    )
                }
            }

            highlightIndexes
                .forEach { highLightIndex ->
                    if (highLightIndex.endIndex <= length) {
                        addStyle(
                            style = SpanStyle(
                                background = nodeData.colorScheme.highlight,
                                color = nodeData.colorScheme.onHighlight,
                                fontFamily = nodeData.typography.body02.fontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            start = highLightIndex.startIndex,
                            end = highLightIndex.endIndex
                        )
                    }
                }
        }
    }
    annotatedString.append(updatedAnnotatedString)
    return updatedMentions
}

fun highlightText(nodeData: NodeData, text: String): AnnotatedString {
    var highlightIndexes = emptyList<MatchQueryResult>()

    if (nodeData.searchQuery.isNotBlank()) {
        highlightIndexes = QueryMatchExtractor.extractQueryMatchIndexes(
            matchText = nodeData.searchQuery,
            text = text
        )
    }

    return buildAnnotatedString {
        append(text)
        highlightIndexes
            .forEach { highLightIndex ->
                if (highLightIndex.endIndex <= length) {
                    addStyle(
                        style = SpanStyle(
                            background = nodeData.colorScheme.highlight,
                            color = nodeData.colorScheme.onPrimaryVariant,
                            fontFamily = nodeData.typography.body02.fontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        start = highLightIndex.startIndex,
                        end = highLightIndex.endIndex
                    )
                }
            }
    }
}

private fun convertTypoGraphs(literal: String) = literal
    .replace("(c)", "©")
    .replace("(C)", "©")
    .replace("(r)", "®")
    .replace("(R)", "®")
    .replace("(tm)", "™")
    .replace("(TM)", "™")
    .replace("+-", "±")
