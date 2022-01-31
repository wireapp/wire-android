package com.wire.android.ui.main.conversation.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.wire.android.ui.common.SurfaceBackgroundWrapper
import com.wire.android.ui.theme.wireDimensions


@Composable
fun RowItem(content: @Composable RowScope.() -> Unit) {
    SurfaceBackgroundWrapper(modifier = Modifier.padding(MaterialTheme.wireDimensions.conversationItemPadding)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(MaterialTheme.wireDimensions.conversationItemRowPadding)
        ) {
            content()
        }
    }
}

