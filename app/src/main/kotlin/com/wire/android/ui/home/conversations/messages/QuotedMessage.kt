package com.wire.android.ui.home.conversations.messages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.model.ImageAsset
import com.wire.android.ui.common.StatusBox
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.typography
import com.wire.android.ui.home.conversations.model.QuotedMessageUIData
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.ui.UIText

@Composable
internal fun QuotedMessage(
    messageData: QuotedMessageUIData
) {
    // Draw content
    when (val quotedContent = messageData.quotedContent) {
        QuotedMessageUIData.Invalid -> QuotedInvalid()

        is QuotedMessageUIData.GenericAsset -> QuotedGenericAsset(
            senderName = messageData.senderName,
            originalDateTimeText = messageData.originalMessageDateDescription,
            assetName = quotedContent.assetName
        )

        is QuotedMessageUIData.DisplayableImage -> QuotedImage(
            senderName = messageData.senderName,
            asset = quotedContent.displayable,
            originalDateTimeText = messageData.originalMessageDateDescription
        )

        QuotedMessageUIData.Deleted -> QuotedDeleted(
            senderName = messageData.senderName,
            originalDateDescription = messageData.originalMessageDateDescription
        )

        is QuotedMessageUIData.Text -> QuotedText(
            text = quotedContent.value,
            editedTimeDescription = messageData.editedTimeDescription,
            originalDateTimeDescription = messageData.originalMessageDateDescription,
            senderName = messageData.senderName
        )
    }
}

@Composable
@Suppress("LongParameterList")
private fun QuotedMessageContent(
    senderName: String?,
    modifier: Modifier = Modifier,
    endContent: @Composable (modifier: Modifier) -> Unit = {},
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
            ).padding(dimensions().spacing8x).fillMaxWidth()
    ) {

        Column(verticalArrangement = Arrangement.spacedBy(dimensions().spacing4x)) {
            QuotedMessageTopRow(senderName)
            Row(horizontalArrangement = Arrangement.spacedBy(dimensions().spacing4x)) {
                startContent()
                Column(verticalArrangement = Arrangement.spacedBy(dimensions().spacing4x)) {
                    centerContent()
                    footerContent()
                }
            }
        }

        // Make sure the end content is all the way to the end by spacing it
        Spacer(modifier = modifier.weight(1f).fillMaxWidth())
        endContent(Modifier.align(Alignment.CenterVertically))
    }
}

@Composable
private fun QuotedMessageTopRow(senderName: String?) {
    Row(horizontalArrangement = Arrangement.spacedBy(dimensions().spacing2x)) {
        Icon(
            painter = painterResource(id = R.drawable.ic_reply),
            tint = colorsScheme().secondaryText,
            contentDescription = null,
            modifier = Modifier.size(dimensions().messageQuoteIconSize)
        )
        senderName?.let {
            Text(text = senderName, style = typography().label02, color = colorsScheme().secondaryText)
        }
    }
}

@Composable
private fun QuotedInvalid() {
    QuotedMessageContent(null, centerContent = {
        StatusBox(stringResource(R.string.label_quote_invalid_or_not_found))
    })
}

@Composable
private fun QuotedDeleted(
    senderName: String,
    originalDateDescription: UIText
) {
    QuotedMessageContent(
        senderName,
        centerContent = {
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
    senderName: String
) {
    QuotedMessageContent(
        senderName,
        centerContent = {
            editedTimeDescription?.let {
                StatusBox(it.asString())
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
) {
    QuotedMessageContent(senderName, endContent = { modifier ->
        Image(
            painter = asset.paint(),
            contentDescription = stringResource(R.string.content_description_image_message),
            modifier = modifier
                .width(dimensions().spacing56x)
                .height(dimensions().spacing56x)
                .clip(RoundedCornerShape(dimensions().spacing8x)),
            alignment = Alignment.Center,
            contentScale = ContentScale.Crop
        )
    }, centerContent = {
        MainContentText(stringResource(R.string.notification_shared_picture))
    }, footerContent = {
        QuotedMessageOriginalDate(originalDateTimeText)
    })
}

@Composable
private fun MainContentText(text: String) {
    Text(text = text, style = typography().subline01)
}

@Composable
private fun QuotedGenericAsset(
    senderName: String,
    originalDateTimeText: UIText,
    assetName: String?,
) {
    QuotedMessageContent(senderName = senderName, centerContent = {
        assetName?.let {
            MainContentText(it)
        }
    }, endContent = { modifier ->
        Icon(
            painter = painterResource(R.drawable.ic_file),
            contentDescription = null,
            modifier = modifier
                .width(dimensions().spacing24x)
                .width(dimensions().spacing24x)
                .size(dimensions().spacing24x),
            tint = colorsScheme().secondaryText
        )
    }, footerContent = {
        QuotedMessageOriginalDate(originalDateTimeText)
    })
}
