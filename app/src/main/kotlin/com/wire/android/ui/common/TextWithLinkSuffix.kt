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
package com.wire.android.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun TextWithLinkSuffix(
    text: AnnotatedString,
    linkText: String? = null,
    onLinkClick: () -> Unit = {},
    linkTag: String = "link",
    textStyle: TextStyle = MaterialTheme.wireTypography.body01,
    textColor: Color = MaterialTheme.wireColorScheme.onBackground,
    linkStyle: TextStyle = MaterialTheme.wireTypography.body02,
    linkColor: Color = MaterialTheme.wireColorScheme.primary,
    linkDecoration: TextDecoration = TextDecoration.Underline,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val linkId = "link"
    val inlineText = if (linkText != null) {
        text.plus(
            buildAnnotatedString {
                append(" ")
                appendInlineContent(linkId, "[link]")
            }
        )
    } else text
    val inlineContent = buildMap {
        if (linkText != null) {
            val textLayoutResult: TextLayoutResult = textMeasurer.measure(
                text = linkText,
                style = linkStyle.copy(textDecoration = linkDecoration),
            )
            val textSize = textLayoutResult.size
            val density = LocalDensity.current
            val (linkWidthSp, linkHeightSp) = with(density) { textSize.width.toSp() to textSize.height.toSp() }

            put(linkId, InlineTextContent(
                placeholder = Placeholder(
                    width = linkWidthSp,
                    height = linkHeightSp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Bottom
                ),
                children = {
                    Text(
                        text = linkText,
                        style = linkStyle,
                        color = linkColor,
                        textDecoration = linkDecoration,
                        modifier = Modifier
                            .testTag(linkTag)
                            .clickable(onClick = onLinkClick)
                    )
                }
            )
            )
        }
    }

    Text(
        text = inlineText,
        style = textStyle,
        color = textColor,
        inlineContent = inlineContent,
        onTextLayout = onTextLayout,
        modifier = modifier,
    )
}

@Composable
private fun PreviewTextWithLinkSuffixBuilder(
    textLines: List<String> = listOf("This is a text with a link"),
    linkText: String = "link",
    calculateWidth: (lastTextLineWidthDp: Dp, linkWidthDp: Dp) -> Dp
) {
    val textStyle = MaterialTheme.wireTypography.body01
    val linkStyle = MaterialTheme.wireTypography.body02.copy(textDecoration = TextDecoration.Underline)
    val textMeasurer = rememberTextMeasurer()
    val lastTextLineLayoutResult = textMeasurer.measure(text = "${textLines.last()} ", style = textStyle)
    val linkLayoutResult = textMeasurer.measure(text = linkText, style = linkStyle)
    val density = LocalDensity.current
    val lastTextLineWidthDp = with(density) { lastTextLineLayoutResult.size.width.toDp() }
    val linkWidthDp = with(density) { linkLayoutResult.size.width.toDp() }
    TextWithLinkSuffix(
        text = AnnotatedString(textLines.joinToString(separator = "\n")),
        linkText = linkText,
        onLinkClick = {},
        modifier = Modifier.width(calculateWidth(lastTextLineWidthDp, linkWidthDp))
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewTextWithLinkSuffixWithoutALink() = WireTheme {
    TextWithLinkSuffix(text = AnnotatedString("This is a text without a link"))
}

@PreviewMultipleThemes
@Composable
fun PreviewTextWithLinkSuffixFittingInSameLine() = WireTheme {
    PreviewTextWithLinkSuffixBuilder { lastTextLineWidthDp, linkWidthDp -> lastTextLineWidthDp + linkWidthDp }
}

@PreviewMultipleThemes
@Composable
fun PreviewTextWithLinkSuffixNotFittingInSameLine() = WireTheme {
    PreviewTextWithLinkSuffixBuilder { lastTextLineWidthDp, linkWidthDp -> lastTextLineWidthDp + (linkWidthDp / 2) }
}

@PreviewMultipleThemes
@Composable
fun PreviewTextWithLinkSuffixMultilineFittingInLastLine() = WireTheme {
    PreviewTextWithLinkSuffixBuilder(
        textLines = listOf("This is a text with a link", "This is a text with a"),
        linkText = "link",
    ) { lastTextLineWidthDp, linkWidthDp -> lastTextLineWidthDp + linkWidthDp }
}

@PreviewMultipleThemes
@Composable
fun PreviewTextWithLinkSuffixMultilineNotFittingInLastLine() = WireTheme {
    PreviewTextWithLinkSuffixBuilder(
        textLines = listOf("This is a text with a", "This is a text with a"),
        linkText = "link"
    ) { lastTextLineWidthDp, linkWidthDp -> lastTextLineWidthDp + (linkWidthDp / 2) }
}
