package com.wire.android.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wire.android.ui.home.conversationslist.common.RowItem

@Composable
fun RowItemTemplate(
    leadingIcon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    subTitle: @Composable () -> Unit = {},
    actions: @Composable () -> Unit = {},
    onRowItemClicked: () -> Unit,
    onRowItemLongClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    RowItem(
        onRowItemClick = onRowItemClicked,
        onRowItemLongClick = onRowItemLongClicked,
        modifier = modifier
    ) {
        leadingIcon()
        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            title()
            subTitle()
        }
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .padding(end = 8.dp)
        ) {
            actions()
        }
    }
}
