package com.wire.android.ui.main.conversationlist.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        Column(
            modifier = Modifier
                .wrapContentWidth()
                .padding(end = 128.dp)
                .background(color = Color.Red)
        ) {
            title()
            subTitle()
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 8.dp)
                .background(color = Color.Green)
        ) {
            if (eventType != null) {
                EventBadgeFactory(eventType = eventType, modifier = Modifier.align(Alignment.TopEnd))
            }
        }
    }
}
