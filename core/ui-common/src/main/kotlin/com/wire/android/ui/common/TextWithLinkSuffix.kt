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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.DpSize
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@Composable
fun TextWithLinkSuffix(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    linkText: String? = null,
    onLinkClick: () -> Unit = {},
    textStyle: TextStyle = MaterialTheme.wireTypography.body01,
    textColor: Color = MaterialTheme.wireColorScheme.onBackground,
    linkStyle: TextStyle = MaterialTheme.wireTypography.body02,
    linkColor: Color = MaterialTheme.wireColorScheme.primary,
    linkDecoration: TextDecoration = TextDecoration.Underline,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    var linkPosition by remember { mutableStateOf(Offset(0f, 0f)) }
    val (inlineText, inlineContent) = buildInlineText(
        text = text,
        linkText = linkText,
        linkStyle = linkStyle,
        linkDecoration = linkDecoration,
        onLinkPositionCalculated = { linkPosition = it }
    )

    // For some reason automation tests can't find inlined text content, so it needs to be added directly in layout. Inlined text content
    // is still used to get the size of the link text and its position so that it works no matter what locale is used. Position of the link
    // is then used in this layout and the proper text composable that can be found by automation tests is placed where it should be.
    Layout(
        modifier = modifier,
        content = {
            Text(
                text = inlineText,
                style = textStyle,
                color = textColor,
                inlineContent = inlineContent,
                onTextLayout = onTextLayout,
                modifier = Modifier.layoutId("text")
            )

            if (linkText != null) {
                Text(
                    text = linkText,
                    style = linkStyle,
                    color = linkColor,
                    textDecoration = linkDecoration,
                    modifier = Modifier.layoutId("link")
                        .clickable(onClick = onLinkClick)
                )
            }
        },
        measurePolicy = { measurables, constraints ->
            val measureConstraints = constraints.copy(minWidth = 0, minHeight = 0)
            val textPlaceable = measurables.first { it.layoutId == "text" }.measure(measureConstraints)
            val linkPlaceable = measurables.firstOrNull { it.layoutId == "link" }?.measure(measureConstraints)
            layout(width = textPlaceable.width, height = textPlaceable.height) {
                textPlaceable.placeRelative(0, 0)
                linkPlaceable?.place(linkPosition.x.toInt(), linkPosition.y.toInt())
            }
        }
    )
}

@Composable
private fun buildInlineText(
    text: AnnotatedString,
    linkText: String?,
    linkStyle: TextStyle,
    linkDecoration: TextDecoration,
    onLinkPositionCalculated: (Offset) -> Unit
): Pair<AnnotatedString, Map<String, InlineTextContent>> {
    val textMeasurer = rememberTextMeasurer()
    val linkId = "link"
    val inlineText = linkText?.let {
        text.plus(
            buildAnnotatedString {
                append(" ")
                appendInlineContent(linkId, "[link]")
            }
        )
    } ?: text
    val inlineContent = buildMap {
        if (linkText != null) {
            val textLayoutResult: TextLayoutResult = textMeasurer.measure(
                text = linkText,
                style = linkStyle.copy(textDecoration = linkDecoration),
            )
            val density = LocalDensity.current
            val (linkWidthSp, linkHeightSp) = with(density) {
                textLayoutResult.size.width.toSp() to textLayoutResult.size.height.toSp()
            }
            val linkSizeDp = with(density) {
                DpSize(textLayoutResult.size.width.toDp(), textLayoutResult.size.height.toDp())
            }
            put(
                linkId,
                InlineTextContent(
                    placeholder = Placeholder(
                        width = linkWidthSp,
                        height = linkHeightSp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Bottom
                    ),
                    children = {
                        Box(
                            modifier = Modifier // It's only a placeholder as well, just to get the real size and position of the link.
                            .size(linkSizeDp)
                            .onGloballyPositioned { it.parentLayoutCoordinates?.let { onLinkPositionCalculated(it.positionInParent()) } }
                        )
                    }
                )
            )
        }
    }
    return inlineText to inlineContent
}
