package com.wire.android.ui.main.conversation.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wire.android.R
import com.wire.android.ui.main.conversation.all.model.EventType
import com.wire.android.ui.theme.WireColor


@Composable
fun EventBadgeFactory(eventType: EventType, modifier: Modifier = Modifier) {
    when (eventType) {
        EventType.MissedCall -> MissedCallBadge(modifier)
        EventType.UnreadMention -> UnreadMentionBadge(modifier)
        is EventType.UnreadMessage -> UnreadMessageEventBadge(unreadMessageCount = eventType.unreadMessageCount, modifier)
        EventType.UnreadReply -> UnreadReplyBadge(modifier)
    }
}

@Composable
fun MissedCallBadge(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.ic_event_badge_missed_call),
        contentDescription = null,
        modifier = modifier
    )
}

@Composable
fun UnreadMentionBadge(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.ic_event_badge_unread_mention),
        contentDescription = null,
        modifier = modifier
    )
}

@Composable
fun UnreadReplyBadge(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.ic_event_badge_unread_reply),
        contentDescription = null,
        modifier = modifier
    )
}

@Composable
fun UnreadMessageEventBadge(unreadMessageCount: Int, modifier: Modifier = Modifier) {
    if (unreadMessageCount > 0) {
        Text(
            text = unReadMessageCountStringify(unreadMessageCount),
            color = Color.White,
            style = TextStyle(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.W700,
                letterSpacing = 0.25.sp,
                fontStyle = FontStyle.Normal,
                lineHeight = 14.sp
            ),
            modifier = modifier
                .wrapContentWidth()
                .background(
                    color = WireColor.Dark90Gray,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(
                    start = 8.dp,
                    top = 2.dp,
                    bottom = 2.dp,
                    end = 8.dp
                )
        )
    }
}

private const val MAX_UNREAD_MESSAGE_COUNT = 99

private fun unReadMessageCountStringify(unreadMessageCount: Int) =
    if (unreadMessageCount > MAX_UNREAD_MESSAGE_COUNT) "$MAX_UNREAD_MESSAGE_COUNT+" else unreadMessageCount.toString()


