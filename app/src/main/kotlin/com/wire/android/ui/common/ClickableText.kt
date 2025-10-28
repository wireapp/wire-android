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

import android.os.Build
import android.text.SpannableString
import android.text.style.URLSpan
import android.text.util.Linkify
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.appLogger

@Composable
fun ClickableText(
    text: AnnotatedString,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
    onLongClick: (() -> Unit)? = null
) {
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    // to provide current lambdas and states otherwise it will use the ones it got when starting a LaunchedEffect inside pointerInput
    val currentOnClick by rememberUpdatedState(newValue = onClick)
    val currentOnLongClick by rememberUpdatedState(newValue = onLongClick)
    val currentLayoutResult by rememberUpdatedState(layoutResult)

    // even though the rememberUpdateState, should be working, we still do not get the reference to the correct
    // lambda's mainly when we scroll to the top of the list and then back to the bottom, it looks like, the reference
    // does not get updated to point to the correct lambda. currentLayoutResult, should be good enough as the key
    // because it should be updated with each "ClickableText" also we are referencing it inside pointerInput.
    val pressIndicator = Modifier.pointerInput(currentLayoutResult.value) {
        detectTapGestures(
            onTap = { pos ->
                currentLayoutResult.value?.let { layoutResult ->
                    currentOnClick(layoutResult.getOffsetForPosition(pos))
                }
            },
            onLongPress = { currentOnLongClick?.invoke() }
        )
    }
    Text(
        text = text,
        modifier = modifier.then(pressIndicator),
        color = color,
        textDecoration = textDecoration,
        textAlign = textAlign,
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

data class LinkInfo(
    val url: String,
    val start: Int,
    val end: Int
)

class LinkSpannableString(source: CharSequence) : SpannableString(source) {
    companion object {
        fun getLinkInfos(text: String, mask: Int): List<LinkInfo> {
            val linkSpannableString = LinkSpannableString(text)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Linkify.addLinks(linkSpannableString, mask) { str: String -> URLSpan(str) }
            } else {
                Linkify.addLinks(linkSpannableString, mask)
            }
            return linkSpannableString.linkInfos
        }
    }

    private inner class Data(
        val what: Any?,
        val start: Int,
        val end: Int
    )

    private val spanList = mutableListOf<Data>()

    private val linkInfos: List<LinkInfo>
        get() = spanList.mapNotNull { data ->
            (data.what as? URLSpan)?.let { urlSpan ->
                LinkInfo(urlSpan.url, data.start, data.end)
            }
        }

    override fun removeSpan(what: Any?) {
        super.removeSpan(what)
        spanList.removeAll { it.what == what }
    }

    override fun setSpan(what: Any?, start: Int, end: Int, flags: Int) {
        if (start >= 0 && end <= this.length && start <= end) {
            super.setSpan(what, start, end, flags)
            spanList.add(Data(what, start, end))
        } else {
            appLogger.e("[LinkSpannableString] Invalid span indices: start=$start, end=$end, length=$length")
        }
    }
}
