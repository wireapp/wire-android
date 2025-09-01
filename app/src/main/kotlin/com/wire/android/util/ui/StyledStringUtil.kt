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

package com.wire.android.util.ui

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

// To be used in Composables when we have access to the styles and colors.
@Suppress("LongParameterList", "SpreadOperator")
fun Resources.stringWithStyledArgs(
    @StringRes stringResId: Int,
    normalStyle: TextStyle,
    argsStyle: TextStyle,
    normalColor: Color,
    argsColor: Color,
    vararg formatArgs: String
): AnnotatedString {
    val normalSpanStyle = toSpanStyle(normalStyle, normalColor)
    val boldSpanStyle = toSpanStyle(argsStyle, argsColor)
    val string = this.getString(stringResId, *formatArgs.map { it.markdownBold() }.toTypedArray())
    return buildAnnotatedString {
        string.split(BOLD_SEPARATOR).forEachIndexed { index, text ->
            withStyle(if (index % 2 == 0) normalSpanStyle else boldSpanStyle) { append(text) }
        }
    }
}

@Suppress("LongParameterList", "SpreadOperator")
fun markdownText(markdownInput: String, style: MarkdownTextStyle): AnnotatedString {
    // The text gets split into pieces based on **
    val splitText = markdownInput.split(BOLD_SEPARATOR).filter { it.isNotEmpty() }

    // Prepare the annotated string
    return buildAnnotatedString {
        splitText.forEach { piece ->
            when {
                markdownInput.contains(BOLD_SEPARATOR + piece.trim() + BOLD_SEPARATOR) -> { // If the piece was between ** characters
                    pushStyle(style = toSpanStyle(style.boldStyle, style.boldColor))
                    append(piece)
                    pop()
                }

                else -> {
                    pushStyle(style = toSpanStyle(style.normalStyle, style.normalColor))
                    append(piece)
                    pop()
                }
            }
        }
    }
}

data class MarkdownTextStyle(val normalStyle: TextStyle, val boldStyle: TextStyle, val normalColor: Color, val boldColor: Color)

fun toSpanStyle(textStyle: TextStyle, color: Color) = SpanStyle(
    color = color,
    fontWeight = textStyle.fontWeight,
    fontSize = textStyle.fontSize,
    fontFamily = textStyle.fontFamily,
    fontStyle = textStyle.fontStyle,
    textDecoration = textStyle.textDecoration,
)

fun String.markdownBold() = this.replace(
    regex = "(?<prefix>^\\s*)(?<text>(\\S(.*\\S)?))(?<suffix>\\s*\$)".toRegex(), // match the text with leading and trailing whitespaces
    replacement = "\${prefix}$BOLD_SEPARATOR\${text}$BOLD_SEPARATOR\${suffix}" // put separators around the text
)

private const val BOLD_SEPARATOR: String = "**"

data class LinkTextData(
    val text: String,
    val tag: String? = null,
    val annotation: String? = null,
    val onClick: ((str: AnnotatedString.Range<String>) -> Unit)? = null,
)

@Composable
fun LinkText(
    linkTextData: List<LinkTextData>,
    modifier: Modifier = Modifier,
    textColor: Color = Color.Unspecified
) {
    val annotatedString = createAnnotatedString(linkTextData)

    ClickableText(
        text = annotatedString,
        style = MaterialTheme.wireTypography.body01.copy(color = textColor),
        onClick = { offset ->
            linkTextData.forEach { annotatedStringData ->
                if (annotatedStringData.tag != null && annotatedStringData.annotation != null) {
                    annotatedString.getStringAnnotations(
                        tag = annotatedStringData.tag,
                        start = offset,
                        end = offset,
                    ).firstOrNull()?.let {
                        annotatedStringData.onClick?.invoke(it)
                    }
                }
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun createAnnotatedString(data: List<LinkTextData>): AnnotatedString {
    return buildAnnotatedString {
        data.forEach { linkTextData ->
            if (linkTextData.tag != null && linkTextData.annotation != null) {
                pushStringAnnotation(
                    tag = linkTextData.tag,
                    annotation = linkTextData.annotation,
                )
                withLink(
                    LinkAnnotation.Url(
                        linkTextData.annotation,
                        TextLinkStyles(
                            style = SpanStyle(
                                color = MaterialTheme.wireColorScheme.primary,
                                textDecoration = TextDecoration.Underline,
                            )
                        )
                    )
                ) {
                    append(linkTextData.text)
                }
                pop()
            } else {
                append("${linkTextData.text} ")
            }
        }
    }
}
