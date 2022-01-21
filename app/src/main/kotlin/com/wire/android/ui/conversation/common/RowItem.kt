package com.wire.android.ui.conversation.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wire.android.ui.common.SurfaceBackgroundWrapper
import com.wire.android.ui.theme.Dimensions


@Composable
fun RowItem(content: @Composable RowScope.() -> Unit) {
    SurfaceBackgroundWrapper(modifier = Modifier.padding(0.5.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(Dimensions.CONVERSATION_ITEM_ROW_PADDING)
        ) {
            content()
        }
    }
}

