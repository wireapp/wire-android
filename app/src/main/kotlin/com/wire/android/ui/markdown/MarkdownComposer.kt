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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.wire.android.appLogger
import com.wire.android.ui.common.divider.WireDivider
import com.wire.android.ui.theme.WireColorScheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableBody
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TableHead
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
import org.commonmark.node.ListBlock
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.ThematicBreak
import org.commonmark.node.Text as nodeText

@Composable
fun MDDocument(document: Document) {
    MDBlockChildren(document)
}

@Composable
fun MDBlockChildren(parent: Node) {
    var child = parent.firstChild
    while (child != null) {
        appLogger.d("KBX $child")
        when (child) {
            is Document -> MDDocument(child)
            is BlockQuote -> MDBlockQuote(child)
            is ThematicBreak -> MDThematicBreak(child)
            is Heading -> MDHeading(child)
            is Paragraph -> MDParagraph(child)
            is FencedCodeBlock -> MDFencedCodeBlock(child)
            is IndentedCodeBlock -> MDIndentedCodeBlock(child)
            is Image -> MDImage(child)
            is BulletList -> MDBulletList(child)
            is OrderedList -> MDOrderedList(child)
            is TableBlock -> MDTable(child)
        }
        child = child.next
    }
}

@Composable
@Suppress("MagicNumber")
fun MDHeading(heading: Heading) {
    val style = when (heading.level) {
        1 -> MaterialTheme.typography.headlineLarge
        2 -> MaterialTheme.typography.headlineMedium
        3 -> MaterialTheme.typography.headlineSmall
        4 -> MaterialTheme.typography.bodyLarge
        5 -> MaterialTheme.typography.bodyMedium
        6 -> MaterialTheme.typography.bodySmall
        else -> {
            // Invalid header...
            MDBlockChildren(heading)
            return
        }
    }
    val padding = if (heading.parent is Document) 8.dp else 0.dp
    Box(modifier = Modifier.padding(bottom = padding)) {
        val text = buildAnnotatedString {
            inlineChildren(heading, this, MaterialTheme.wireColorScheme)
        }
        MarkdownText(text, style)
    }
}

@Composable
fun MDParagraph(paragraph: Paragraph) {
    if (paragraph.firstChild is Image && paragraph.firstChild == paragraph.lastChild) {
        // Paragraph with single image
        MDImage(paragraph.firstChild as Image)
    } else {
        val padding = if (paragraph.parent is Document) 8.dp else 0.dp
        Box(modifier = Modifier.padding(bottom = padding)) {
            val styledText = buildAnnotatedString {
                pushStyle(MaterialTheme.wireTypography.body01.toSpanStyle())
                inlineChildren(paragraph, this, MaterialTheme.wireColorScheme)
                pop()
            }
            MarkdownText(styledText, MaterialTheme.wireTypography.body01)
        }
    }
}

@Composable
fun MDImage(image: Image) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
//        CoilImage(data = image.destination)
    }
}

@Composable
fun MDBulletList(bulletList: BulletList) {
    val marker = "\u2022"
    MDListItems(bulletList) {
        val text = buildAnnotatedString {
            pushStyle(MaterialTheme.wireTypography.body01.toSpanStyle())
            append("$marker ")
            inlineChildren(it, this, MaterialTheme.wireColorScheme)
            pop()
        }
        MarkdownText(text, MaterialTheme.wireTypography.body01)
    }
}

@Composable
fun MDOrderedList(orderedList: OrderedList) {
    var number = orderedList.startNumber
    val delimiter = orderedList.delimiter
    MDListItems(orderedList) {
        val text = buildAnnotatedString {
            pushStyle(MaterialTheme.wireTypography.body01.toSpanStyle())
            append("${number++}$delimiter ")
            inlineChildren(it, this, MaterialTheme.wireColorScheme)
            pop()
        }
        MarkdownText(text, MaterialTheme.wireTypography.body01)
    }
}

@Composable
fun MDListItems(listBlock: ListBlock, item: @Composable (node: Node) -> Unit) {
    val bottom = if (listBlock.parent is Document) 8.dp else 0.dp
    val start = if (listBlock.parent is Document) 0.dp else 8.dp
    Column(modifier = Modifier.padding(start = start, bottom = bottom)) {
        var listItem = listBlock.firstChild
        while (listItem != null) {
            var child = listItem.firstChild
            while (child != null) {
                when (child) {
                    is BulletList -> MDBulletList(child)
                    is OrderedList -> MDOrderedList(child)
                    else -> item(child)
                }
                child = child.next
            }
            listItem = listItem.next
        }
    }
}

@Composable
fun MDBlockQuote(blockQuote: BlockQuote) {
    val color = MaterialTheme.wireColorScheme.onBackground
    Column(modifier = Modifier
        .drawBehind {
            drawLine(
                color = color,
                strokeWidth = 2f,
                start = Offset(12.dp.value, 0f),
                end = Offset(12.dp.value, size.height)
            )
        }
        .padding(start = 16.dp, top = 4.dp, bottom = 4.dp)) {

        var child = blockQuote.firstChild
        while (child != null) {
            when (child) {
                is BlockQuote -> MDBlockQuote(child)
                else -> {
                    val text = buildAnnotatedString {
                        pushStyle(
                            MaterialTheme.wireTypography.body01.toSpanStyle()
                                .plus(SpanStyle(fontStyle = FontStyle.Italic))
                        )
                        inlineChildren(child, this, MaterialTheme.wireColorScheme)
                        pop()
                    }
                    Text(text)
                }
            }
            child = child.next
        }
    }
}

@Composable
fun MDFencedCodeBlock(fencedCodeBlock: FencedCodeBlock) {
    appLogger.d("KBY ${fencedCodeBlock.info}")
    Text(
        text = fencedCodeBlock.literal,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .background(Color.Gray.copy(alpha = 0.2f))
            .border(1.dp, Color.LightGray, shape = RoundedCornerShape(4.dp))
            .padding(4.dp)
    )
}

@Composable
fun MDIndentedCodeBlock(indentedCodeBlock: IndentedCodeBlock) {
    Text(
        text = indentedCodeBlock.literal,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .background(Color.Gray.copy(alpha = 0.2f))
            .border(1.dp, Color.LightGray, shape = RoundedCornerShape(4.dp))
            .padding(4.dp)
    )
}

@Composable
fun MDThematicBreak(thematicBreak: ThematicBreak) {
    WireDivider(MaterialTheme.wireColorScheme.outline, modifier = Modifier.padding(vertical = 8.dp))
}

fun inlineChildren(
    parent: Node,
    annotatedString: AnnotatedString.Builder,
    colors: WireColorScheme
) {
    var child = parent.firstChild
    while (child != null) {
        when (child) {
            is Paragraph -> inlineChildren(
                child,
                annotatedString,
                colors
            )

            is nodeText -> annotatedString.append(
                child.literal
                    .replace("(c)", "©")
                    .replace("(C)", "©")
                    .replace("(r)", "®")
                    .replace("(R)", "®")
                    .replace("(tm)", "™")
                    .replace("(TM)", "™")
                    .replace("+-", "±")
            )

            is Image -> {
                annotatedString.appendInlineContent(TAG_IMAGE_URL, child.destination)
            }

            is Emphasis -> {
                annotatedString.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                inlineChildren(
                    child,
                    annotatedString,
                    colors
                )
                annotatedString.pop()
            }

            is StrongEmphasis -> {
                annotatedString.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                inlineChildren(
                    child,
                    annotatedString,
                    colors
                )
                annotatedString.pop()
            }

            is Code -> {
                annotatedString.pushStyle(TextStyle(fontFamily = FontFamily.Monospace, background = Color.Green).toSpanStyle())
                annotatedString.append(child.literal)
                annotatedString.pop()
            }

            is HardLineBreak -> {
                annotatedString.pushStyle(TextStyle(fontFamily = FontFamily.Monospace).toSpanStyle())
                annotatedString.append("\n")
                annotatedString.pop()
            }

            is Link -> {
                annotatedString.pushStyle(
                    SpanStyle(
                        color = colors.primary,
                        textDecoration = TextDecoration.Underline
                    )
                )
                annotatedString.pushStringAnnotation(TAG_URL, child.destination)
                inlineChildren(
                    child,
                    annotatedString,
                    colors
                )
                annotatedString.pop()
                annotatedString.pop()
            }

            is Strikethrough -> {
                annotatedString.pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                inlineChildren(child, annotatedString, colors)
                annotatedString.pop()
            }
        }
        child = child.next
    }
}

@Composable
fun MDTable(tableBlock: TableBlock) {
    val tableData = mutableListOf<List<AnnotatedString>>()
    var child = tableBlock.firstChild
    // Parse the table block
    while (child != null) {
        when (child) {
            is TableHead -> {
                var rowNode = child.firstChild
                while (rowNode != null) {
                    val row = parseRow(rowNode, MaterialTheme.wireColorScheme)
                    tableData.add(row)
                    rowNode = rowNode.next
                }
            }

            is TableBody -> {
                var rowNode = child.firstChild
                while (rowNode != null) {
                    val row = parseRow(rowNode, MaterialTheme.wireColorScheme)
                    tableData.add(row)
                    rowNode = rowNode.next
                }
            }
        }
        child = child.next
    }

    val columnCount = tableData.firstOrNull()?.size ?: 0

    // Create a table
    Column {
        tableData.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (rowIndex == 0) Color.LightGray else Color.White)
            ) {
                for (columnIndex in 0 until columnCount) {
                    Text(
                        text = row[columnIndex].toString(),
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp)
                            .border(0.5.dp, Color.Gray)
                    )
                }
            }
        }
    }
}

private fun parseRow(tableRow: Node, colors: WireColorScheme): List<AnnotatedString> {
    val row = mutableListOf<AnnotatedString>()
    var child = tableRow.firstChild
    while (child != null) {
        if (child is TableCell) {
            val cellText = buildAnnotatedString {
                inlineChildren(child, this, colors)
            }
            row.add(cellText)
        }
        child = child.next
    }
    return row
}

@Composable
fun MarkdownText(text: AnnotatedString, style: TextStyle) {
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text,
        style = style,
        onTextLayout = { layoutResult.value = it },
        maxLines = Int.MAX_VALUE
    )
}

private const val TAG_URL = "url"
private const val TAG_IMAGE_URL = "imageUrl"
