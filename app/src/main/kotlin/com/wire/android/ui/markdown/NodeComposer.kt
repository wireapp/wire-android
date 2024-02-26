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
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.wire.android.ui.markdown.MarkdownConstants.TAG_URL

@Composable
fun NodeDocument(
    document: MarkdownNode.Document,
    nodeData: NodeData,
    clickable: Boolean
) {
    val filteredDocument = if (nodeData.searchQuery.isNotBlank()) {
        document.filterNodesContainingQuery(nodeData.searchQuery)
    } else {
        document
    }
    println("AFTER FILTER")

    printMarkdownNodeTree(filteredDocument)
    if (filteredDocument != null) {
        MarkdownNodeBlockChildren(
            (filteredDocument as MarkdownNode.Document).children,
            nodeData,
            clickable
        )
    }
}

@Composable
fun MarkdownNodeBlockChildren(
    children: List<MarkdownNode.Block>,
    nodeData: NodeData,
    clickable: Boolean = true
) {
    var updateMentions = nodeData.mentions
    val updatedNodeData = nodeData.copy(mentions = updateMentions)

    Column {
        children.map { node ->
            when (node) {
                is MarkdownNode.Block.BlockQuote -> MarkdownNodeBlockQuote(node, updatedNodeData)
                is MarkdownNode.Block.IntendedCode -> MarkdownNodeIndentedCodeBlock(indentedCodeBlock = node, nodeData = updatedNodeData)
                is MarkdownNode.Block.FencedCode -> MarkdownNodeFencedCodeBlock(fencedCodeBlock = node, nodeData = updatedNodeData)
                is MarkdownNode.Block.Heading -> MarkdownNodeHeading(heading = node, nodeData = updatedNodeData)
                is MarkdownNode.Block.ListBlock.Ordered -> MarkdownNodeOrderedList(orderedList = node, nodeData = updatedNodeData)
                is MarkdownNode.Block.ListBlock.Bullet -> MarkdownNodeBulletList(bulletList = node, nodeData = updatedNodeData)

                is MarkdownNode.Block.Paragraph -> MarkdownNodeParagraph(
                    paragraph = node, nodeData = updatedNodeData,
                    clickable
                ) {
                    updateMentions = it
                }

                is MarkdownNode.Block.Table -> MarkdownNodeTable(node, updatedNodeData) {
                    updateMentions = it
                }

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
                updatedMentions = appendLinksAndMentions(
                    annotatedString,
                    convertTypoGraphs(child.literal),
                    nodeData.copy(mentions = updatedMentions)
                )
            }

            is MarkdownNode.Inline.Image -> {
                updatedMentions = appendLinksAndMentions(
                    annotatedString,
                    child.destination,
                    nodeData.copy(mentions = updatedMentions)
                )
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
                annotatedString.pushStyle(
                    SpanStyle(
                        color = nodeData.colorScheme.primary,
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
