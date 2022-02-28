package com.wire.android.ui.home.conversationslist.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.wire.android.ui.common.SurfaceBackgroundWrapper
import com.wire.android.ui.theme.wireDimensions


//TODO: added onRowClick only for UI-Design purpose
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RowItem(
    onRowItemClick: () -> Unit,
    onRowItemLongClick: () -> Unit,
    content: @Composable (RowScope.() -> Unit),
) {
    SurfaceBackgroundWrapper(modifier = Modifier.padding(MaterialTheme.wireDimensions.conversationItemPadding)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(MaterialTheme.wireDimensions.conversationItemRowHeight)
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onRowItemClick() },
                    onLongClick = { onRowItemLongClick() }
                )
        ) {
            content()
        }
    }
}

