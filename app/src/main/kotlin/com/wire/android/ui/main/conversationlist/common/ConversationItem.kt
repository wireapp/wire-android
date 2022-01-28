package com.wire.android.ui.main.conversationlist.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wire.android.ui.main.conversationlist.model.EventType

@Composable
fun ConversationItem(
    avatar: @Composable () -> Unit,
    title: @Composable () -> Unit,
    subTitle: @Composable () -> Unit = {},
    eventType: EventType? = null
) {
    RowItem(onRowItemClick = {}) {
        avatar()
        Column(modifier = Modifier.wrapContentWidth()) {
            title()
            subTitle()
        }
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .wrapContentSize()
                .padding(end = 8.dp)
        ) {
            if (eventType != null) {
                EventBadgeFactory(eventType = eventType)
            }
        }
    }
}
