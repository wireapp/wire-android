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

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.wire.android.ui.home.conversations.messages.item.interceptCombinedClickable
import com.wire.android.ui.home.conversations.model.MessageSenderId
import com.wire.android.ui.markdown.MarkdownConstants.TAG_MENTION
import com.wire.android.ui.markdown.MarkdownConstants.TAG_URL
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.common.R as commonR

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
    onOpenProfile: ((senderId: MessageSenderId) -> Unit)? = null
) {

    val mentionActions = annotatedString.getMentions(onOpenProfile)
    val accessibilityModifier = if (clickable && mentionActions.isNotEmpty()) {
        modifier.semantics {
            customActions = mentionActions
        }
    } else {
        modifier
    }

    val currentOnClickLink by rememberUpdatedState(onClickLink)
    val currentOnOpenProfile by rememberUpdatedState(onOpenProfile)
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val linkStyles = TextLinkStyles(
        focusedStyle = SpanStyle(
            background = MaterialTheme.wireColorScheme.inverseSurface,
            color = MaterialTheme.wireColorScheme.inverseOnSurface,
        )
    )
    val inlineFocus = rememberInlineActionFocus()
    val currentSaveFocus by rememberUpdatedState(inlineFocus.save)
    val text = remember(annotatedString, clickable, onClickLink != null, onOpenProfile != null, linkStyles) {
        if (clickable) {
            annotatedString.withInlineActions(
                linkStyles = linkStyles,
                onActivate = { currentSaveFocus(it) },
                onClickLink = onClickLink?.let { { url -> currentOnClickLink?.invoke(url) } },
                onOpenProfile = onOpenProfile?.let { { sender -> currentOnOpenProfile?.invoke(sender) } },
            )
        } else {
            annotatedString
        }
    }
    // Observe long presses without consuming link taps. Message containers also intercept long
    // presses, including on links, before the Text's clickable children handle the pointer event.
    val longClickModifier = if (clickable && onLongClick != null) {
        Modifier.interceptCombinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onLongPress = { currentOnLongClick?.invoke() },
        )
    } else {
        Modifier
    }
    Text(
        text = text,
        modifier = accessibilityModifier
            .then(if (text.getLinkAnnotations(0, text.length).isNotEmpty()) inlineFocus.modifier else Modifier)
            .then(longClickModifier),
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

@Composable
private fun AnnotatedString.getMentions(onOpenProfile: ((senderId: MessageSenderId) -> Unit)? = null): List<CustomAccessibilityAction> {
    val openProfileDescription = stringResource(id = commonR.string.content_description_open_user_profile_label)
    return getStringAnnotations(TAG_MENTION, 0, length)
        .mapNotNull { annotation ->
            onOpenProfile?.let {
                val mentionText = text.substring(annotation.start, annotation.end)
                CustomAccessibilityAction("$openProfileDescription $mentionText") {
                    it(MessageSenderId.User(annotation.item))
                    true
                }
            }
        }
}

/** Keep the original text/styles and turn each actionable range into a native inline target. */
@Suppress("NestedBlockDepth")
internal fun AnnotatedString.withInlineActions(
    linkStyles: TextLinkStyles,
    onClickLink: ((String) -> Unit)?,
    onOpenProfile: ((MessageSenderId) -> Unit)?,
    onActivate: (Int) -> Unit = {},
): AnnotatedString {
    val actions = getStringAnnotations(0, length)
        .filter {
            it.start < it.end && ((it.tag == TAG_URL && onClickLink != null) || (it.tag == TAG_MENTION && onOpenProfile != null))
        }
        // Explicit Markdown links enclose automatic URL annotations in their label. The outer
        // range owns the action; do not create overlapping native link layout nodes.
        .sortedWith(compareBy<AnnotatedString.Range<String>> { it.start }.thenByDescending { it.end })
    return AnnotatedString.Builder(this).apply {
        var previousEnd = -1
        var targetIndex = 0
        actions.forEach { action ->
            if (action.start >= previousEnd) {
                val index = targetIndex++
                when (action.tag) {
                    TAG_URL -> addLink(
                        LinkAnnotation.Url(action.item, linkStyles) {
                            onActivate(index)
                            onClickLink?.invoke(action.item)
                        },
                        action.start,
                        action.end,
                    )
                    else -> addLink(
                        LinkAnnotation.Clickable("$TAG_MENTION:${action.start}:${action.item}", linkStyles) {
                            onActivate(index)
                            onOpenProfile?.invoke(MessageSenderId.User(action.item))
                        },
                        action.start,
                        action.end,
                    )
                }
                previousEnd = action.end
            }
        }
    }.toAnnotatedString()
}
