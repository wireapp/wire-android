package com.wire.android.ui.home.conversations.messages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.model.ImageAsset
import com.wire.android.ui.common.StatusBox
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.typography
import com.wire.android.ui.home.conversations.messages.QuotedMessageStyle.COMPLETE
import com.wire.android.ui.home.conversations.messages.QuotedMessageStyle.PREVIEW
import com.wire.android.ui.home.conversations.model.QuotedMessageUIData
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.ui.UIText

private const val TEXT_QUOTE_MAX_LINES = 7

/**
 * Indicates whether this QuotedMessage should display all the possible details or not.
 */
enum class QuotedMessageStyle {
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
    messageData: QuotedMessageUIData,
    style: QuotedMessageStyle = COMPLETE,
    modifier: Modifier = Modifier,
    startContent: @Composable () -> Unit = {}
) {
    // Draw content
    when (val quotedContent = messageData.quotedContent) {
        QuotedMessageUIData.Invalid -> QuotedInvalid(style)

        is QuotedMessageUIData.GenericAsset -> QuotedGenericAsset(
            senderName = messageData.senderName,
            originalDateTimeText = messageData.originalMessageDateDescription,
            assetName = quotedContent.assetName,
            modifier = modifier,
            style = style,
            startContent = startContent
        )

        is QuotedMessageUIData.DisplayableImage -> QuotedImage(
            senderName = messageData.senderName,
            asset = quotedContent.displayable,
            originalDateTimeText = messageData.originalMessageDateDescription,
            modifier = modifier,
            style = style,
            startContent = startContent
        )

        QuotedMessageUIData.Deleted -> QuotedDeleted(
            senderName = messageData.senderName,
            originalDateDescription = messageData.originalMessageDateDescription,
            modifier = modifier,
            style = style,
        )

        is QuotedMessageUIData.Text -> QuotedText(
            text = quotedContent.value,
            editedTimeDescription = messageData.editedTimeDescription,
            originalDateTimeDescription = messageData.originalMessageDateDescription,
            senderName = messageData.senderName,
            modifier = modifier,
            style = style,
            startContent = startContent
        )
    }
}

@Composable
fun QuotedMessagePreview(
    quotedMessageData: QuotedMessageUIData,
    onCancelReply: () -> Unit
) {
    QuotedMessage(quotedMessageData, style = PREVIEW) {
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
    centerContent: @Composable () -> Unit = {}
) {
    val quoteOutlineShape = RoundedCornerShape(dimensions().messageAssetBorderRadius)
    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensions().spacing4x),
        modifier = modifier
            .background(
                color = MaterialTheme.wireColorScheme.surface,
                shape = quoteOutlineShape
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.wireColorScheme.divider,
                shape = quoteOutlineShape
            ).padding(dimensions().spacing4x).fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(start = dimensions().spacing4x)) {
            startContent()
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(dimensions().spacing4x),
            modifier = Modifier.padding(vertical = dimensions().spacing4x)
        ) {
            QuotedMessageTopRow(senderName, displayReplyArrow = style == COMPLETE)
            Row(horizontalArrangement = Arrangement.spacedBy(dimensions().spacing4x)) {
                Column(verticalArrangement = Arrangement.spacedBy(dimensions().spacing4x)) {
                    centerContent()
                    if (style == COMPLETE) {
                        footerContent()
                    }
                }
            }
        }

        // Make sure the end content is all the way to the end by spacing it
        Spacer(modifier = modifier.weight(1f).fillMaxWidth())
        val endContentExtraPadding = if (style == COMPLETE) dimensions().spacing4x else dimensions().spacing0x
        Box(
            modifier = Modifier.align(Alignment.CenterVertically)
                .padding(bottom = endContentExtraPadding, top = endContentExtraPadding, end = endContentExtraPadding)
        ) {
            endContent()
        }
    }
}

@Composable
private fun QuotedMessageTopRow(
    senderName: String?,
    displayReplyArrow: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensions().spacing2x),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (displayReplyArrow) {
            Icon(
                painter = painterResource(id = R.drawable.ic_reply),
                tint = colorsScheme().secondaryText,
                contentDescription = null,
                modifier = Modifier.size(dimensions().messageQuoteIconSize),
            )
        }
        senderName?.let {
            Text(text = senderName, style = typography().label02, color = colorsScheme().secondaryText)
        }
    }
}

@Composable
private fun QuotedInvalid(style: QuotedMessageStyle) {
    QuotedMessageContent(null, style = style, centerContent = {
        StatusBox(stringResource(R.string.label_quote_invalid_or_not_found))
    })
}

@Composable
private fun QuotedDeleted(
    senderName: String,
    originalDateDescription: UIText,
    style: QuotedMessageStyle,
    modifier: Modifier = Modifier,
    startContent: @Composable () -> Unit = {},
) {
    QuotedMessageContent(
        senderName,
        style = style,
        modifier = modifier,
        startContent = {
            startContent()
        }, centerContent = {
            StatusBox(stringResource(R.string.deleted_message_text))
        }, footerContent = {
            QuotedMessageOriginalDate(originalDateDescription)
        }
    )
}

@Composable
private fun QuotedText(
    text: String,
    editedTimeDescription: UIText?,
    originalDateTimeDescription: UIText,
    senderName: String,
    modifier: Modifier = Modifier,
    startContent: @Composable () -> Unit = {},
    style: QuotedMessageStyle
) {
    QuotedMessageContent(
        senderName,
        style = style,
        modifier = modifier,
        startContent = {
            startContent()
        }, centerContent = {
            editedTimeDescription?.let {
                if (style == COMPLETE) {
                    StatusBox(it.asString())
                }
            }
            MainContentText(text)
        }, footerContent = {
            QuotedMessageOriginalDate(originalDateTimeDescription)
        }
    )
}

@Composable
private fun QuotedMessageOriginalDate(
    originalDateTimeText: UIText
) {
    Text(originalDateTimeText.asString(), style = typography().subline01, color = colorsScheme().secondaryText)
}

@Composable
private fun QuotedImage(
    senderName: String,
    asset: ImageAsset.PrivateAsset,
    originalDateTimeText: UIText,
    startContent: @Composable () -> Unit = {},
    style: QuotedMessageStyle,
    modifier: Modifier
) {
    val imageDimension = if (style == COMPLETE) dimensions().spacing56x else dimensions().spacing40x
    QuotedMessageContent(senderName, style = style, modifier = modifier, endContent = {
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
    }, startContent = {
        startContent()
    }, centerContent = {
        MainContentText(stringResource(R.string.notification_shared_picture))
    }, footerContent = {
        QuotedMessageOriginalDate(originalDateTimeText)
    })
}

@Composable
private fun MainContentText(text: String) {
    Text(
        text = text,
        style = typography().subline01,
        maxLines = TEXT_QUOTE_MAX_LINES,
        overflow = TextOverflow.Ellipsis,
        color = colorsScheme().secondaryText
    )
}

@Composable
private fun QuotedGenericAsset(
    senderName: String,
    originalDateTimeText: UIText,
    assetName: String?,
    style: QuotedMessageStyle,
    startContent: @Composable () -> Unit = {},
    modifier: Modifier
) {
    QuotedMessageContent(
        senderName = senderName, style = style, modifier = modifier, centerContent = {
            assetName?.let {
                MainContentText(it)
            }
        }, startContent = {
            startContent()
        }, endContent = {
            Icon(
                painter = painterResource(R.drawable.ic_file),
                contentDescription = null,
                modifier = modifier
                    .width(dimensions().spacing24x)
                    .width(dimensions().spacing24x)
                    .size(dimensions().spacing24x),
                tint = colorsScheme().secondaryText
            )
        }, footerContent = { QuotedMessageOriginalDate(originalDateTimeText) }
    )
}
