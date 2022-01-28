package com.wire.android.ui.main.conversationlist.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun GroupConversationAvatar(colorValue: ULong) {
    Box(
        modifier = Modifier
            .size(width = 24.dp, height = 24.dp)
            .background(color = Color(colorValue))
    )
}
