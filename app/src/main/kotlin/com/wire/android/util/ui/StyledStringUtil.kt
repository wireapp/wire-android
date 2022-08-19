package com.wire.android.util.ui

import android.content.res.Resources
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
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

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

private fun toSpanStyle(textStyle: TextStyle, color: Color) = SpanStyle(
    color = color,
    fontWeight = textStyle.fontWeight,
    fontSize = textStyle.fontSize,
    fontFamily = textStyle.fontFamily,
    fontStyle = textStyle.fontStyle
)

private fun String.bold() = STYLE_SEPARATOR + this + STYLE_SEPARATOR

private const val STYLE_SEPARATOR: String = "\u0000"


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
) {
    val annotatedString = createAnnotatedString(linkTextData)

    ClickableText(
        text = annotatedString,
        style = MaterialTheme.wireTypography.body01,
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
                        color = MaterialTheme.wireColorScheme.checkedBoxColor,
                        textDecoration = TextDecoration.Underline,
                    ),
                ) {
                    append(linkTextData.text)
                }
                pop()
            } else {
                append(linkTextData.text)
            }
        }
    }
}
