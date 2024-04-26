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

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.wire.android.ui.common.ClickableText
import com.wire.android.ui.markdown.MarkdownConstants.TAG_MENTION
import com.wire.android.ui.markdown.MarkdownConstants.TAG_URL

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
    onLongClick: (() -> Unit)? = null,
    onOpenProfile: ((String) -> Unit)? = null
) {

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
                    onClickLink?.invoke(result.item)
                }

                annotatedString.getStringAnnotations(
                    tag = TAG_MENTION,
                    start = offset,
                    end = offset
                ).firstOrNull()?.let { result ->
                    onOpenProfile?.invoke(result.item)
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
