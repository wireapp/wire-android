package com.wire.android.ui.home.conversationslist.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@Composable
fun ReplyMessage(
    replyAuthor: String,
    replyBody: String,
    onCancelReply: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .heightIn(max = 80.dp)
            .padding(8.dp)
            .wrapContentHeight()
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.wireColorScheme.divider,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Image(
            colorFilter = ColorFilter.tint(MaterialTheme.wireColorScheme.secondaryText),
            modifier = Modifier
                .padding(
                    start = 16.dp,
                    end = 8.dp,
                    top = 16.dp,
                    bottom = 16.dp
                )
                .width(16.dp)
                .height(16.dp)
                .clickable(onClick = onCancelReply),
            painter = painterResource(
                id = R.drawable.ic_close
            ),
            contentDescription = "Cancel message reply"
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(8.dp)
        ) {
            ReplyAuthor(replyAuthor)
            Spacer(Modifier.height(4.dp))
            ReplyBody(replyBody)
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
