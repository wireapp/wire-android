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
 *
 *
 */

package com.wire.android.util.ui

import android.content.res.Resources
import android.text.SpannedString
import androidx.annotation.StringRes
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
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
    val string = this.getString(stringResId, *formatArgs.map { it.bold() }.toTypedArray())
    return buildAnnotatedString {
        string.split(STYLE_SEPARATOR).forEachIndexed { index, text ->
            withStyle(if (index % 2 == 0) normalSpanStyle else boldSpanStyle) { append(text) }
        }
    }
}

@Suppress("LongParameterList", "SpreadOperator")
fun Resources.annotatedText(
    stringResId: LocalizedStringResource,
    normalStyle: TextStyle,
    boldStyle: TextStyle,
    normalColor: Color,
    boldColor: Color,
    errorColor: Color,
    isErrorString: Boolean,
    vararg formatArgs: String
): AnnotatedString {

    // Mark all arguments as bold, by adding **
    val input = when (stringResId) {
        is LocalizedStringResource.PluralResource -> this.getQuantityString(
            stringResId.id,
            stringResId.quantity,
            *formatArgs.map { it.markdownBold() }.toTypedArray()
        )

        is LocalizedStringResource.StringResource -> this.getString(stringResId.id, *formatArgs.map { it.markdownBold() }.toTypedArray())
    }
    // The text gets split into pieces based on **
    val splitText = input.split(BOLD_SEPARATOR).filter { it.isNotEmpty() }

    // Prepare the annotated string
    return buildAnnotatedString {
        splitText.forEach { piece ->
            when {
                input.contains(BOLD_SEPARATOR + piece.trim() + BOLD_SEPARATOR) -> { // If the piece was between ** characters
                    pushStyle(style = toSpanStyle(boldStyle, useErrorColorIfApplies(isErrorString, errorColor, boldColor)))
                    append(piece)
                    pop()
                }

                else -> {
                    pushStyle(style = toSpanStyle(normalStyle, useErrorColorIfApplies(isErrorString, errorColor, normalColor)))
                    append(piece)
                    pop()
                }
            }
        }
    }
}

private fun useErrorColorIfApplies(isErrorString: Boolean, errorColor: Color, regularColor: Color) =
    if (isErrorString) errorColor else regularColor

// To be used outside of Composables, e.g. in notifications.
@Suppress("LongParameterList", "SpreadOperator")
fun Resources.stringWithBoldArgs(
    @StringRes stringResId: Int,
    vararg formatArgs: String
): SpannedString {
    val string = this.getString(stringResId, *formatArgs.map { it.bold() }.toTypedArray())
    return buildSpannedString {
        string.split(STYLE_SEPARATOR).forEachIndexed { index, text ->
            if (index % 2 == 0) append(text)
            else bold { append(text) }
        }
    }
}

private fun toSpanStyle(textStyle: TextStyle, color: Color) = SpanStyle(
    color = color,
    fontWeight = textStyle.fontWeight,
    fontSize = textStyle.fontSize,
    fontFamily = textStyle.fontFamily,
    fontStyle = textStyle.fontStyle,
    textDecoration = textStyle.textDecoration,
)

private fun String.bold() = STYLE_SEPARATOR + this + STYLE_SEPARATOR
private fun String.markdownBold() = BOLD_SEPARATOR + this + BOLD_SEPARATOR

private const val STYLE_SEPARATOR: String = "\u0000"
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
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.wireColorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                    ),
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
