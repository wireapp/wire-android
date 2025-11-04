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
@file:Suppress("TooManyFunctions")

package com.wire.android.ui.home.conversations.messages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.model.ImageAsset
import com.wire.android.ui.common.StatusBox
import com.wire.android.ui.common.clickable
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.typography
import com.wire.android.ui.home.conversations.messages.item.MessageStyle
import com.wire.android.ui.home.conversations.messages.item.highlighted
import com.wire.android.ui.home.conversations.model.UIQuotedMessage
import com.wire.android.ui.markdown.MarkdownInline
import com.wire.android.ui.markdown.MessageColors
import com.wire.android.ui.markdown.NodeData
import com.wire.android.ui.markdown.getFirstInlines
import com.wire.android.ui.markdown.toMarkdownDocument
import com.wire.android.ui.theme.Accent
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.UIText

private const val TEXT_QUOTE_MAX_LINES = 7

data class QuotedMessageStyle(
    val quotedStyle: QuotedStyle,
    val messageStyle: MessageStyle,
    val selfAccent: Accent
)

/**
 * Indicates whether this QuotedMessage should display all the possible details or not.
 */
enum class QuotedStyle {
    /**
     * Will attempt to display all possible information in normal size.
     */
    COMPLETE,

    /**
     * Will keep only the most important information, hiding secondary UI elements like:
     * - Footer with date/time
     * - Edit time
     * - Reply Icon
     *
     * Images have a reduced size.
     */
    PREVIEW
}

@Composable
internal fun QuotedMessage(
    messageData: UIQuotedMessage.UIQuotedData,
    clickable: Clickable?,
    style: QuotedMessageStyle,
    modifier: Modifier = Modifier,
    startContent: @Composable () -> Unit = {}
) {

    when (val quotedContent = messageData.quotedContent) {
        UIQuotedMessage.UIQuotedData.Invalid -> QuotedInvalid(style)

        is UIQuotedMessage.UIQuotedData.GenericAsset -> QuotedGenericAsset(
            senderName = messageData.senderName,
            originalDateTimeText = messageData.originalMessageDateDescription,
            assetName = quotedContent.assetName,
            modifier = modifier,
            style = style,
            startContent = startContent,
            clickable = clickable
        )

        is UIQuotedMessage.UIQuotedData.DisplayableImage -> QuotedImage(
            senderName = messageData.senderName,
            asset = quotedContent.displayable,
            originalDateTimeText = messageData.originalMessageDateDescription,
            modifier = modifier,
            style = style,
            startContent = startContent,
            clickable = clickable
        )

        UIQuotedMessage.UIQuotedData.Deleted -> QuotedDeleted(
            senderName = messageData.senderName,
            originalDateDescription = messageData.originalMessageDateDescription,
            modifier = modifier,
            style = style,
            clickable = clickable
        )

        is UIQuotedMessage.UIQuotedData.Text -> QuotedText(
            text = quotedContent.value.asString(),
            editedTimeDescription = messageData.editedTimeDescription,
            originalDateTimeDescription = messageData.originalMessageDateDescription,
            senderName = messageData.senderName,
            modifier = modifier,
            style = style,
            startContent = startContent,
            clickable = clickable
        )

        is UIQuotedMessage.UIQuotedData.AudioMessage -> QuotedAudioMessage(
            senderName = messageData.senderName,
            originalDateTimeText = messageData.originalMessageDateDescription,
            modifier = modifier,
            style = style,
            startContent = startContent,
            clickable = clickable
        )

        is UIQuotedMessage.UIQuotedData.Location -> QuotedLocation(
            senderName = messageData.senderName,
            originalDateTimeText = messageData.originalMessageDateDescription,
            locationName = quotedContent.locationName,
            modifier = modifier,
            style = style,
            startContent = startContent,
            clickable = clickable
        )
    }
}

@Composable
fun QuotedMessagePreview(
    quotedMessageData: UIQuotedMessage.UIQuotedData,
    onCancelReply: () -> Unit,
    modifier: Modifier = Modifier
) {
    QuotedMessage(
        modifier = modifier,
        messageData = quotedMessageData,
        clickable = null,
        style = QuotedMessageStyle(QuotedStyle.PREVIEW, MessageStyle.NORMAL, Accent.Unknown)
    ) {
        Box(
            modifier = Modifier
                .padding(
                    vertical = dimensions().spacing8x,
                    horizontal = dimensions().spacing4x
                )
                .width(dimensions().spacing40x)
                .height(dimensions().spacing32x)
                .clickable(onClick = onCancelReply),
            contentAlignment = Alignment.Center
        ) {
            Image(
                colorFilter = ColorFilter.tint(MaterialTheme.wireColorScheme.secondaryText),
                modifier = Modifier
                    .width(dimensions().spacing16x)
                    .height(dimensions().spacing16x),
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = stringResource(R.string.content_description_reply_cancel)
            )
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun QuotedMessageContent(
    senderName: String?,
    style: QuotedMessageStyle,
    modifier: Modifier = Modifier,
    endContent: @Composable () -> Unit = {},
    startContent: @Composable () -> Unit = {},
    footerContent: @Composable () -> Unit = {},
    centerContent: @Composable () -> Unit = {},
    clickable: Clickable? = null
) {
    val quoteOutlineShape = RoundedCornerShape(dimensions().messageAssetBorderRadius)
    val background = when (style.messageStyle) {
        MessageStyle.BUBBLE_SELF -> colorsScheme().selfBubble.secondary
        MessageStyle.BUBBLE_OTHER -> colorsScheme().otherBubble.secondary
        MessageStyle.NORMAL -> MaterialTheme.wireColorScheme.surfaceVariant
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensions().spacing4x),
        modifier = modifier
            .background(
                color = background,
                shape = quoteOutlineShape
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.wireColorScheme.outline,
                shape = quoteOutlineShape
            )
            .padding(dimensions().spacing4x)
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .let {
                if (clickable != null) {
                    it
                        .clickable(clickable)
                } else {
                    it
                }
            }
    ) {
        Box(modifier = Modifier.padding(start = dimensions().spacing4x)) {
            startContent()
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensions().spacing4x),
            modifier = Modifier
                .padding(vertical = dimensions().spacing4x)
                .weight(1.0f) // Fill the remaining space
        ) {
            QuotedMessageTopRow(
                senderName,
                displayReplyArrow = style.quotedStyle == QuotedStyle.COMPLETE,
                messageStyle = style.messageStyle
            )
            Row(horizontalArrangement = Arrangement.spacedBy(dimensions().spacing4x)) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(dimensions().spacing4x)
                ) {
                    centerContent()
                    if (style.quotedStyle == QuotedStyle.COMPLETE) {
                        footerContent()
                    }
                }
            }
        }
        val endContentExtraPadding = if (style.quotedStyle == QuotedStyle.COMPLETE) dimensions().spacing4x else dimensions().spacing0x
        Box(
            modifier = Modifier
                .padding(
                    bottom = endContentExtraPadding,
                    top = endContentExtraPadding,
                    end = endContentExtraPadding
                )
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            endContent()
        }
    }
}

@Composable
private fun QuotedMessageTopRow(
    senderName: String?,
    displayReplyArrow: Boolean,
    messageStyle: MessageStyle
) {

    val authorColor = when (messageStyle) {
        MessageStyle.BUBBLE_SELF -> colorsScheme().selfBubble.primaryOnSecondary
        MessageStyle.BUBBLE_OTHER -> colorsScheme().otherBubble.primaryOnSecondary
        MessageStyle.NORMAL -> colorsScheme().onSurfaceVariant
    }

    val replyIconColor = when (messageStyle) {
        MessageStyle.BUBBLE_SELF -> colorsScheme().selfBubble.onSecondary
        MessageStyle.BUBBLE_OTHER -> colorsScheme().otherBubble.onSecondary
        MessageStyle.NORMAL -> colorsScheme().secondaryText
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensions().spacing2x),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (displayReplyArrow) {
            Icon(
                painter = painterResource(id = R.drawable.ic_reply),
                tint = replyIconColor,
                contentDescription = null,
                modifier = Modifier.size(dimensions().messageQuoteIconSize),
            )
        }
        senderName?.let {
            Text(text = senderName, style = typography().label02, color = authorColor)
        }
    }
}

@Composable
fun QuotedUnavailable(style: QuotedMessageStyle) {
    QuotedMessageContent(
        stringResource(id = R.string.username_unavailable_label),
        style = style,
        centerContent = {
            MainContentText(stringResource(R.string.label_quote_invalid_or_not_found), fontStyle = FontStyle.Italic)
        }
    )
}

@Composable
fun QuotedInvalid(style: QuotedMessageStyle) {
    QuotedMessageContent(
        senderName = null,
        style = style,
        centerContent = {
            StatusBox(stringResource(R.string.label_quote_invalid_or_not_found))
        }
    )
}

@Composable
private fun QuotedDeleted(
    senderName: UIText,
    originalDateDescription: UIText,
    style: QuotedMessageStyle,
    clickable: Clickable?,
    modifier: Modifier = Modifier,
    startContent: @Composable () -> Unit = {}
) {
    QuotedMessageContent(
        senderName.asString(),
        style = style,
        modifier = modifier,
        startContent = {
            startContent()
        },
        centerContent = {
            StatusBox(stringResource(R.string.deleted_message_text))
        },
        footerContent = {
            QuotedMessageOriginalDate(originalDateDescription, style)
        },
        clickable = clickable
    )
}

@Composable
private fun QuotedText(
    text: String,
    editedTimeDescription: UIText?,
    originalDateTimeDescription: UIText,
    senderName: UIText,
    style: QuotedMessageStyle,
    clickable: Clickable?,
    modifier: Modifier = Modifier,
    startContent: @Composable () -> Unit = {}
) {
    QuotedMessageContent(
        senderName = senderName.asString(),
        style = style,
        modifier = modifier,
        startContent = {
            startContent()
        },
        centerContent = {
            editedTimeDescription?.let {
                if (style.quotedStyle == QuotedStyle.COMPLETE) {
                    StatusBox(it.asString())
                }
            }
            MainMarkdownText(text, messageStyle = style.messageStyle, accent = style.selfAccent)
        },
        footerContent = {
            QuotedMessageOriginalDate(originalDateTimeDescription, style)
        },
        clickable = clickable
    )
}

@Composable
private fun QuotedMessageOriginalDate(
    originalDateTimeText: UIText,
    style: QuotedMessageStyle
) {
    val color = when (style.messageStyle) {
        MessageStyle.BUBBLE_SELF -> colorsScheme().selfBubble.onSecondary
        MessageStyle.BUBBLE_OTHER -> colorsScheme().otherBubble.onSecondary
        MessageStyle.NORMAL -> colorsScheme().secondaryText
    }
    Text(
        originalDateTimeText.asString(),
        style = typography().subline01,
        color = color,
    )
}

@Composable
private fun QuotedImage(
    senderName: UIText,
    asset: ImageAsset.PrivateAsset,
    originalDateTimeText: UIText,
    style: QuotedMessageStyle,
    clickable: Clickable?,
    modifier: Modifier = Modifier,
    startContent: @Composable () -> Unit = {}
) {

    if (style.quotedStyle == QuotedStyle.PREVIEW) {

        // Standard quoted message layout
        val imageDimension = dimensions().spacing40x
        QuotedMessageContent(
            senderName = senderName.asString(),
            style = style,
            modifier = modifier,
            endContent = {
                Image(
                    painter = asset.paint(),
                    contentDescription = stringResource(R.string.content_description_image_message),
                    modifier = Modifier
                        .width(imageDimension)
                        .height(imageDimension)
                        .clip(RoundedCornerShape(dimensions().spacing8x)),
                    alignment = Alignment.Center,
                    contentScale = ContentScale.Crop
                )
            },
            startContent = {
                startContent()
            },
            centerContent = {
                MainContentText(stringResource(R.string.notification_shared_picture))
            },
            footerContent = {
                QuotedMessageOriginalDate(originalDateTimeText, style)
            },
            clickable = clickable
        )
    } else {
        val background = when (style.messageStyle) {
            MessageStyle.BUBBLE_SELF -> MaterialTheme.wireColorScheme.primaryVariant
            MessageStyle.BUBBLE_OTHER -> MaterialTheme.wireColorScheme.surface
            MessageStyle.NORMAL -> MaterialTheme.wireColorScheme.surfaceVariant
        }
        // Similar to the standard layout, but the space for the image stretches
        // according to the height of the message content
        val quoteOutlineShape = RoundedCornerShape(dimensions().messageAssetBorderRadius)
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimensions().spacing8x, Alignment.Start),
            modifier = modifier
                .background(
                    color = background,
                    shape = quoteOutlineShape
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.wireColorScheme.outline,
                    shape = quoteOutlineShape
                )
                .padding(dimensions().spacing4x)
                .fillMaxWidth()
        ) {
            // This is the composable that does the trick of stretching the image
            AutosizeContainer(asset = asset, clickable = clickable) {
                QuotedMessageTopRow(
                    senderName = senderName.asString(),
                    displayReplyArrow = true,
                    messageStyle = style.messageStyle
                )
                MainContentText(stringResource(R.string.notification_shared_picture))
                QuotedMessageOriginalDate(originalDateTimeText, style)
            }
        }
    }
}

@Composable
private fun AutosizeContainer(
    asset: ImageAsset.PrivateAsset,
    modifier: Modifier = Modifier,
    clickable: Clickable? = null,
    content: @Composable () -> Unit
) {
    val imageDimension = Dimension.value(dimensions().spacing56x)
    // ConstraintLayout is used to measure the text content and then
    // resize the image to match the height of the text
    ConstraintLayout(
        modifier = modifier
            .fillMaxWidth()
            .padding(dimensions().spacing8x)
            .let {
                if (clickable != null) {
                    it
                        .clickable(clickable)
                } else {
                    it
                }
            }
    ) {
        val (leftSide, rightSide) = createRefs()
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top),
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.constrainAs(leftSide) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
                end.linkTo(rightSide.start)
                width = Dimension.fillToConstraints
                height = Dimension.wrapContent
            }
        ) {
            content()
        }
        Image(
            painter = asset.paint(),
            contentDescription = stringResource(R.string.content_description_image_message),
            modifier = Modifier
                .constrainAs(rightSide) {
                    top.linkTo(leftSide.top)
                    bottom.linkTo(leftSide.bottom)
                    end.linkTo(parent.end)
                    width = imageDimension
                    height = Dimension.fillToConstraints
                }
                .clip(RoundedCornerShape(dimensions().spacing8x))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.wireColorScheme.outline,
                    shape = RoundedCornerShape(dimensions().spacing8x)
                ),
            alignment = Alignment.Center,
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun QuotedAudioMessage(
    senderName: UIText,
    originalDateTimeText: UIText,
    style: QuotedMessageStyle,
    startContent: @Composable () -> Unit,
    clickable: Clickable?,
    modifier: Modifier = Modifier
) {
    QuotedMessageContent(
        senderName = senderName.asString(),
        style = style,
        modifier = modifier,
        centerContent = {
            MainContentText(stringResource(R.string.attachment_voice_message))
        },
        startContent = {
            startContent()
        },
        endContent = {
            Icon(
                painter = painterResource(R.drawable.ic_audio),
                contentDescription = null,
                modifier = Modifier
                    .padding(end = dimensions().spacing16x)
                    .size(dimensions().spacing24x),
                tint = colorsScheme().secondaryText
            )
        },
        footerContent = { QuotedMessageOriginalDate(originalDateTimeText, style) },
        clickable = clickable
    )
}

@Composable
private fun MainMarkdownText(text: String, messageStyle: MessageStyle, accent: Accent, fontStyle: FontStyle = FontStyle.Normal) {

    val color = when (messageStyle) {
        MessageStyle.BUBBLE_SELF -> colorsScheme().selfBubble.onSecondary
        MessageStyle.BUBBLE_OTHER -> colorsScheme().otherBubble.onSecondary
        MessageStyle.NORMAL -> colorsScheme().onSurfaceVariant
    }

    val nodeData = NodeData(
        color = color,
        style = MaterialTheme.wireTypography.subline01.copy(fontStyle = fontStyle),
        colorScheme = MaterialTheme.wireColorScheme,
        typography = MaterialTheme.wireTypography,
        searchQuery = "",
        mentions = listOf(),
        disableLinks = true,
        messageStyle = messageStyle,
        messageColors = MessageColors(highlighted = messageStyle.highlighted()),
        accent = accent
    )

    val markdownPreview = remember(text) {
        text.toMarkdownDocument().getFirstInlines()
    }

    if (markdownPreview != null) {
        MarkdownInline(
            inlines = markdownPreview.children,
            maxLines = TEXT_QUOTE_MAX_LINES,
            nodeData = nodeData
        )
    } else {
        MainContentText(text, fontStyle)
    }
}

@Composable
private fun MainContentText(text: String, fontStyle: FontStyle = FontStyle.Normal) {
    Text(
        text = text,
        style = typography().subline01,
        maxLines = TEXT_QUOTE_MAX_LINES,
        overflow = TextOverflow.Ellipsis,
        color = colorsScheme().onSurfaceVariant,
        fontStyle = fontStyle
    )
}

@Composable
private fun QuotedGenericAsset(
    senderName: UIText,
    originalDateTimeText: UIText,
    assetName: String?,
    style: QuotedMessageStyle,
    clickable: Clickable?,
    modifier: Modifier = Modifier,
    startContent: @Composable () -> Unit = {}
) {
    QuotedMessageContent(
        senderName = senderName.asString(),
        style = style,
        modifier = modifier,
        centerContent = {
            assetName?.let {
                MainContentText(it)
            }
        },
        startContent = {
            startContent()
        },
        endContent = {
            Icon(
                painter = painterResource(R.drawable.ic_file),
                contentDescription = null,
                modifier = Modifier
                    .size(dimensions().spacing24x),
                tint = colorsScheme().secondaryText
            )
        },
        footerContent = { QuotedMessageOriginalDate(originalDateTimeText, style) },
        clickable = clickable
    )
}

@Composable
private fun QuotedLocation(
    senderName: UIText,
    originalDateTimeText: UIText,
    locationName: String,
    style: QuotedMessageStyle,
    clickable: Clickable?,
    modifier: Modifier = Modifier,
    startContent: @Composable () -> Unit = {}
) {
    QuotedMessageContent(
        senderName = senderName.asString(),
        style = style,
        modifier = modifier,
        centerContent = {
            MainContentText(locationName)
        },
        startContent = {
            startContent()
        },
        endContent = {
            Icon(
                painter = painterResource(R.drawable.ic_location),
                contentDescription = null,
                modifier = Modifier
                    .size(dimensions().spacing24x),
                tint = colorsScheme().secondaryText
            )
        },
        footerContent = { QuotedMessageOriginalDate(originalDateTimeText, style) },
        clickable = clickable
    )
}
