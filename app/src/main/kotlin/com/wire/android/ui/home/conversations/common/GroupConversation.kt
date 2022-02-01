package com.wire.android.ui.home.conversations.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.ui.theme.wireTypography

@Composable
fun GroupConversationAvatar(colorValue: Long) {
    Box(
        modifier = Modifier
            .padding(8.dp)
            .size(width = 32.dp, height = 32.dp)
            .background(color = Color(colorValue), shape = RoundedCornerShape(10.dp))
    )
}

@Composable
fun GroupName(name: String) {
    Text(
        text = name,
        style = MaterialTheme.wireTypography.body02
    )
}


@Preview
@Composable
fun GroupConversationAvatarPreview() {
    GroupConversationAvatar(colorValue = 0xFF0000FF)
}
