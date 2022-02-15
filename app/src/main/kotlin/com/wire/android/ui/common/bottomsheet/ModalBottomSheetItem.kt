package com.wire.android.ui.common.bottomsheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wire.android.ui.theme.wireDimensions


@Composable
fun ModalBottomSheetItem(
    icon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    action: @Composable () -> Unit = {},
    onItemClick: () -> Unit = {}
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
        .height(MaterialTheme.wireDimensions.conversationBottomSheetItemHeight)
        .fillMaxWidth()
        .clickable { onItemClick() }
        .padding(MaterialTheme.wireDimensions.conversationBottomSheetItemPadding)
    ) {
        icon()
        Spacer(modifier = Modifier.width(12.dp))
        title()
        Spacer(modifier = Modifier.weight(1f))
        action()
    }
}

// Add Divider() at the beginning of the list, then keep adding at the end of each items, till last item
@Composable
fun buildMenuSheetItems(items: List<@Composable () -> Unit>) {
    Divider()
    items.forEachIndexed { index: Int, itemBuilder: @Composable () -> Unit ->
        itemBuilder()
        if (index != items.size) {
            Divider()
        }
    }
}
