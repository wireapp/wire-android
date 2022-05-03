package com.wire.android.ui.home.conversationslist.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.ui.theme.wireDimensions

@Composable
fun GroupConversationAvatar(colorValue: Long) {
    Box(
        modifier = Modifier
            .padding(8.dp)
            .size(MaterialTheme.wireDimensions.groupAvatarSize)
            .background(color = Color(colorValue), shape = RoundedCornerShape(MaterialTheme.wireDimensions.groupAvatarCornerRadius))
    )
}


@Preview
@Composable
fun GroupConversationAvatarPreview() {
    GroupConversationAvatar(colorValue = 0xFF0000FF)
}
