package com.wire.android.ui.main.conversationlist.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.wire.android.ui.common.SurfaceBackgroundWrapper
import com.wire.android.ui.theme.Dimensions


//TODO: added onRowClick only for UI-Design purpose
@Composable
fun RowItem(onRowItemClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    SurfaceBackgroundWrapper(modifier = Modifier.padding(Dimensions.conversationItemPadding)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(Dimensions.conversationItemRowPadding)
                .clickable {
                    onRowItemClick()
                }
        ) {
            content()
        }
    }
}

