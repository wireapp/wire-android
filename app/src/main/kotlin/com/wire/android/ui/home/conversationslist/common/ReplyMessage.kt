package com.wire.android.ui.home.conversationslist.common

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.home.messagecomposer.MessageReplyType
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@Composable
fun ReplyMessage(
    messageReplyType: MessageReplyType,
    onCancelReply: () -> Unit
) {
    when (messageReplyType) {
        is MessageReplyType.AssetReply -> {
            ReplyContainer(
                replyAuthor = messageReplyType.author,
                replyBody = messageReplyType.assetName,
                onCancelReply = onCancelReply
            )
        }

        is MessageReplyType.ImageReply -> {
            ReplyContainer(
                replyAuthor = messageReplyType.author,
                replyBody = "Picture",
                replyIcon = {
                    Image(
                        painter = messageReplyType.imagePath!!.paint(),
                        contentDescription = stringResource(R.string.content_description_image_message),
                        modifier = Modifier
                            .width(40.dp)
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                },
                onCancelReply = onCancelReply
            )
        }

        is MessageReplyType.TextReply -> {
            ReplyContainer(
                replyAuthor = messageReplyType.author,
                replyBody = messageReplyType.textBody,
                onCancelReply = onCancelReply
            )
        }
    }
}

@Composable
private fun ReplyContainer(
    replyAuthor: String,
    replyBody: String,
    replyIcon: @Composable (() -> Unit)? = null,
    onCancelReply: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(8.dp)
            .wrapContentHeight()
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.wireColorScheme.divider,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .padding(
                    vertical = 8.dp,
                    horizontal = 4.dp
                )
                .width(40.dp)
                .height(32.dp)
                .clickable(onClick = onCancelReply),
            contentAlignment = Alignment.Center
        ) {
            Image(
                colorFilter = ColorFilter.tint(MaterialTheme.wireColorScheme.secondaryText),
                modifier = Modifier
                    .width(16.dp)
                    .height(16.dp),
                painter = painterResource(
                    id = R.drawable.ic_close
                ),
                contentDescription = "Cancel message reply"
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .wrapContentHeight()
                .padding(
                    start = 4.dp,
                    top = 8.dp,
                    bottom = 8.dp,
                    end = 8.dp
                )
        ) {
            ReplyAuthor(replyAuthor)
            Spacer(Modifier.height(2.dp))
            ReplyBody(replyBody)
        }
        if (replyIcon != null) {
            Box(
                modifier = Modifier
                    .wrapContentSize()
            ) {
                replyIcon()
            }
        }
    }
}

@Composable
private fun ReplyBody(body: String) {
    Text(
        style = MaterialTheme.wireTypography.subline01,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.wireColorScheme.secondaryText,
        text = body,
        maxLines = 2
    )
}

@Composable
private fun ReplyAuthor(name: String) {
    Text(
        style = MaterialTheme.wireTypography.label02,
        color = MaterialTheme.wireColorScheme.secondaryText,
        text = name
    )
}
