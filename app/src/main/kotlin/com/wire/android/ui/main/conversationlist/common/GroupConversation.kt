package com.wire.android.ui.main.conversationlist.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wire.android.ui.theme.body02

@Composable
fun GroupConversationAvatar(colorValue: ULong) {
    Box(
        modifier = Modifier
            .size(width = 24.dp, height = 24.dp)
            .background(color = Color(colorValue))
    )
}

@Composable
fun GroupName(name: String) {
    Text(
        text = name,
        style = MaterialTheme.typography.body02
    )
}

