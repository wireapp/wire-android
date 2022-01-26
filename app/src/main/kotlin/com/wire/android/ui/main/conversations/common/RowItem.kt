package com.wire.android.ui.main.conversations.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.wire.android.ui.common.SurfaceBackgroundWrapper
import com.wire.android.ui.theme.Dimensions


@Composable
fun RowItem(content: @Composable RowScope.() -> Unit) {
    SurfaceBackgroundWrapper(modifier = Modifier.padding(Dimensions.conversationItemPadding)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(Dimensions.conversationItemRowPadding)
        ) {
            content()
        }
    }
}

