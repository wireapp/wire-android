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

import android.text.util.Linkify
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.wire.android.appLogger
import com.wire.android.ui.common.ClickableText
import com.wire.android.ui.common.SpannableStr
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.message.mention.MessageMention
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Document
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.node.ThematicBreak
import java.lang.StringBuilder
import org.commonmark.node.Text as nodeText

@Composable
fun MDDocument(document: Document, nodeData: NodeData) {
    MDBlockChildren(document, nodeData)
}

@Composable
fun MDBlockChildren(parent: Node, nodeData: NodeData) {
    var child = parent.firstChild
    while (child != null) {
        when (child) {
            is Document -> MDDocument(child, nodeData)
            is BlockQuote -> MDBlockQuote(child, nodeData)
            is ThematicBreak -> MDThematicBreak()
            is Heading -> MDHeading(child, nodeData)
            is Paragraph -> MDParagraph(child, nodeData)
            is FencedCodeBlock -> MDFencedCodeBlock(child)
            is IndentedCodeBlock -> MDIndentedCodeBlock(child)
            is Image -> MDImage(child)
            is BulletList -> MDBulletList(child, nodeData)
            is OrderedList -> MDOrderedList(child, nodeData)
            is TableBlock -> MDTable(child, nodeData)
        }
        child = child.next
    }
}

@Composable
fun MDParagraph(paragraph: Paragraph, nodeData: NodeData) {
    if (paragraph.firstChild is Image && paragraph.firstChild == paragraph.lastChild) {
        // Paragraph with single image
        MDImage(paragraph.firstChild as Image)
    } else {
        val padding = if (paragraph.parent is Document) dimensions().spacing8x else dimensions().spacing0x
        Box(modifier = Modifier.padding(bottom = padding)) {
            val annotatedString = buildAnnotatedString {
                pushStyle(MaterialTheme.wireTypography.body01.toSpanStyle())
                inlineChildren(paragraph, this, nodeData)
                pop()
            }
            MarkdownText(
                annotatedString,
                style = nodeData.style,
                onLongClick = nodeData.onLongClick,
                onOpenProfile = nodeData.onOpenProfile
            )
        }
    }
}

fun inlineChildren(
    parent: Node,
    annotatedString: AnnotatedString.Builder,
    nodeData: NodeData
) {
    var child = parent.firstChild
    while (child != null) {
        when (child) {
            is Paragraph -> inlineChildren(
                child,
                annotatedString,
                nodeData
            )

            is nodeText -> {
                val textWithTypograps = convertTypoGraphs(child)

                appendLinks(
                    annotatedString,
                    textWithTypograps,
                    nodeData
                )
            }

            is Image -> {
                annotatedString.appendInlineContent(TAG_IMAGE_URL, child.destination)
            }

            is Emphasis -> {
                annotatedString.pushStyle(
                    SpanStyle(
                        fontFamily = nodeData.typography.body05.fontFamily,
                        fontStyle = FontStyle.Italic
                    )
                )
                inlineChildren(
                    child,
                    annotatedString,
                    nodeData
                )
                annotatedString.pop()
            }

            is StrongEmphasis -> {
                annotatedString.pushStyle(
                    SpanStyle(
                        fontFamily = nodeData.typography.body02.fontFamily,
                        fontWeight = FontWeight.Bold
                    )
                )
                inlineChildren(
                    child,
                    annotatedString,
                    nodeData
                )
                annotatedString.pop()
            }

            is Code -> {
                annotatedString.pushStyle(TextStyle(fontFamily = FontFamily.Monospace).toSpanStyle())
                annotatedString.append(child.literal)
                annotatedString.pop()
            }

            is HardLineBreak -> {
                annotatedString.append("\n")
                annotatedString.pop()
            }

            is Link -> {
                annotatedString.pushStyle(
                    SpanStyle(
                        color = nodeData.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )
                )
                annotatedString.pushStringAnnotation(TAG_URL, child.destination)
                inlineChildren(
                    child,
                    annotatedString,
                    nodeData
                )
                annotatedString.pop()
                annotatedString.pop()
            }

            is Strikethrough -> {
                annotatedString.pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                inlineChildren(child, annotatedString, nodeData)
                annotatedString.pop()
            }
        }
        child = child.next
    }
}

private fun convertTypoGraphs(child: Text) = child.literal
    .replace("(c)", "©")
    .replace("(C)", "©")
    .replace("(r)", "®")
    .replace("(R)", "®")
    .replace("(tm)", "™")
    .replace("(TM)", "™")
    .replace("+-", "±")

@Composable
fun MarkdownText(
    annotatedString: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
    clickable: Boolean = true,
    onClickLink: ((linkText: String) -> Unit)? = null,
    onLongClick: (() -> Unit)?,
    onOpenProfile: (String) -> Unit
) {
    val uriHandler = LocalUriHandler.current

    if (clickable) {
        ClickableText(
            text = annotatedString,
            modifier = modifier,
            color = color,
            textAlign = textAlign,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            onTextLayout = onTextLayout,
            style = style,
            onClick = { offset ->
                annotatedString.getStringAnnotations(
                    tag = TAG_URL,
                    start = offset,
                    end = offset,
                ).firstOrNull()?.let { result ->
                    uriHandler.openUri(result.item)
                    onClickLink?.invoke(annotatedString.substring(result.start, result.end))
                }

                annotatedString.getStringAnnotations(
                    tag = TAG_MENTION,
                    start = offset,
                    end = offset
                ).firstOrNull()?.let { result ->
                    onOpenProfile(result.item)
                }
            },
            onLongClick = onLongClick
        )
    } else {
        Text(
            text = annotatedString,
            modifier = modifier,
            color = color,
            textAlign = textAlign,
            lineHeight = lineHeight,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            onTextLayout = onTextLayout,
            style = style
        )
    }

}

fun appendLinks(
    annotatedString: AnnotatedString.Builder,
    string: String,
    nodeData: NodeData
) {
    val linkInfos = SpannableStr.getLinkInfos(string, Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES)
    val stringBuilder = StringBuilder(string)

    // TODO remove [mention] if string contains it and save startIndex for each mentions
    val mentionList: List<MessageMention> = if (stringBuilder.contains("[mention_") && nodeData.mentions.isNotEmpty()) {
        nodeData.mentions.mapNotNull { displayMention ->
            val mentionWithUserId = "[mention_${displayMention.userId}]"
            val length = mentionWithUserId.length
            val startIndex = stringBuilder.indexOf(mentionWithUserId)
            if (startIndex != -1) {
                appLogger.d("KBX index $startIndex length $length $stringBuilder")
                stringBuilder.replace(startIndex, length, "")
                appLogger.d("KBX $stringBuilder")

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


    with(annotatedString) {
        append(stringBuilder)
        with(nodeData.colorScheme) {
            linkInfos.forEach {
                if (it.end - it.start <= 0) {
                    return@forEach
                }
                addStyle(
                    style = SpanStyle(
                        color = primary,
                        textDecoration = TextDecoration.Underline
                    ),
                    start = it.start,
                    end = it.end
                )
                addStringAnnotation(
                    tag = TAG_URL,
                    annotation = it.url,
                    start = it.start,
                    end = it.end
                )
            }
            // TODO use previously saved mention position
            if (mentionList.isNotEmpty()) {
                mentionList.forEach {
                    if (it.length <= 0 || it.start >= length || it.start + it.length > length) {
                        return@forEach
                    }
                    addStyle(
                        style = SpanStyle(
                            fontWeight = nodeData.typography.body02.fontWeight,
                            color = onPrimaryVariant,
                            background = if (it.isSelfMention) primaryVariant else Color.Unspecified
                        ),
                        start = it.start,
                        end = it.start + it.length
                    )
                    addStringAnnotation(
                        tag = TAG_MENTION,
                        annotation = it.userId.toString(),
                        start = it.start,
                        end = it.start + it.length
                    )
                }
            }
        }
    }
}

private const val TAG_URL = "linkTag"
private const val TAG_IMAGE_URL = "imageUrl"
private const val TAG_MENTION = "mentionTag"

