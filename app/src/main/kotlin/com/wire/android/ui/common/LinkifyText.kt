/*
 * Wire
 * Copyright (C) 2021-2023 Wire Swiss GmbH
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

package com.wire.android.ui.common

import android.os.Build
import android.text.SpannableString
import android.text.style.URLSpan
import android.text.util.Linkify
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.ui.UIText

@Suppress("ComplexMethod")
@Composable
fun LinkifyText(
    text: UIText,
    mask: Int = Linkify.ALL,
    modifier: Modifier = Modifier,
    linkEntire: Boolean = false,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
    clickable: Boolean = true,
    onClickLink: ((linkText: String) -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onOpenProfile: (String) -> Unit
) {
    val textAsString = text.asString()
    val uriHandler = LocalUriHandler.current
    val linkInfos = if (linkEntire) {
        listOf(LinkInfo(textAsString, 0, textAsString.length))
    } else {
        SpannableStr.getLinkInfos(textAsString, mask)
    }
    val annotatedString = buildAnnotatedString {
        append(textAsString)
        with(MaterialTheme.wireColorScheme) {
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
                    tag = "linkTag",
                    annotation = it.url,
                    start = it.start,
                    end = it.end
                )
            }
            if (text is UIText.DynamicString && text.mentions.isNotEmpty()) {
                text.mentions.forEach {
                    if (it.length <= 0) {
                        return@forEach
                    }
                    addStyle(
                        style = SpanStyle(
                            fontWeight = typography().body02.fontWeight,
                            color = onPrimaryVariant,
                            background = if (it.isSelfMention) primaryVariant else Color.Unspecified
                        ),
                        start = it.start,
                        end = it.start + it.length
                    )
                    addStringAnnotation(
                        tag = "mentionTag",
                        annotation = it.userId.toString(),
                        start = it.start,
                        end = it.start + it.length
                    )
                }
            }
        }
    }
    if (clickable) {
        ClickableText(
            text = annotatedString,
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            textAlign = textAlign,
            lineHeight = lineHeight,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            onTextLayout = onTextLayout,
            style = style,
            onClick = { offset ->
                annotatedString.getStringAnnotations(
                    tag = "linkTag",
                    start = offset,
                    end = offset,
                ).firstOrNull()?.let { result ->
                    if (linkEntire) {
                        onClickLink?.invoke(annotatedString.substring(result.start, result.end))
                    } else {
                        uriHandler.openUri(result.item)
                        onClickLink?.invoke(annotatedString.substring(result.start, result.end))
                    }
                }

                annotatedString.getStringAnnotations(
                    tag = "mentionTag",
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
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
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

@Composable
private fun ClickableText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
    onClick: (Int) -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    val pressIndicator = Modifier.pointerInput(Unit) {
        detectTapGestures(
            onTap = { pos ->
                layoutResult.value?.let { layoutResult ->
                    onClick(layoutResult.getOffsetForPosition(pos))
                }
            },
            onLongPress = { onLongClick?.invoke() }
        )
    }
    Text(
        text = text,
        modifier = modifier.then(pressIndicator),
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        onTextLayout = {
            layoutResult.value = it
            onTextLayout(it)
        },
        style = style
    )
}

private data class LinkInfo(
    val url: String,
    val start: Int,
    val end: Int
)

private class SpannableStr(source: CharSequence) : SpannableString(source) {
    companion object {
        fun getLinkInfos(text: String, mask: Int = Linkify.ALL): List<LinkInfo> {
            val spannableStr = SpannableStr(text)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Linkify.addLinks(spannableStr, mask) { str: String -> URLSpan(str) }
            } else {
                Linkify.addLinks(spannableStr, mask)
            }
            return spannableStr.linkInfos
        }
    }

    private inner class Data(
        val what: Any?,
        val start: Int,
        val end: Int
    )

    private val spanList = mutableListOf<Data>()

    private val linkInfos: List<LinkInfo>
        get() = spanList.filter { it.what is URLSpan }.map {
            LinkInfo(
                (it.what as URLSpan).url,
                it.start,
                it.end
            )
        }

    override fun removeSpan(what: Any?) {
        super.removeSpan(what)
        spanList.removeAll { it.what == what }
    }

    override fun setSpan(what: Any?, start: Int, end: Int, flags: Int) {
        super.setSpan(what, start, end, flags)
        spanList.add(Data(what, start, end))
    }
}
