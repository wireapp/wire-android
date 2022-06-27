package com.wire.android.util.ui

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

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
private fun toSpanStyle(textStyle: TextStyle, color: Color) = SpanStyle(
    color = color,
    fontWeight = textStyle.fontWeight,
    fontSize = textStyle.fontSize,
    fontFamily = textStyle.fontFamily,
    fontStyle = textStyle.fontStyle
)

private fun String.bold() = STYLE_SEPARATOR + this + STYLE_SEPARATOR

private const val STYLE_SEPARATOR: String = "\u0000"
