package com.wire.android.ui.common.bottomsheet

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import io.github.esentsov.PackagePrivate


@Composable
fun MenuBottomSheetItem(
    title: String,
    icon: @Composable () -> Unit,
    action: (@Composable () -> Unit)? = null,
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
        MenuItemTitle(title = title)
        if (action != null) {
            Spacer(modifier = Modifier.weight(1f))
            action()
        }
    }
}

// Add Divider() at the beginning of the list, then keep adding at the end of each items, till last item
@Composable
fun buildMenuSheetItems(items: List<@Composable () -> Unit>) {
    Divider(thickness = 0.5.dp)
    items.forEachIndexed { index: Int, itemBuilder: @Composable () -> Unit ->
        itemBuilder()
        if (index != items.size) {
            Divider(thickness = 0.5.dp)
        }
    }
}

@PackagePrivate
@Composable
fun MenuItemTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.wireTypography.body01,
        modifier = modifier
    )
}

@Composable
fun MenuItemIcon(
    @DrawableRes id: Int,
    contentDescription: String,
    size: Dp = MaterialTheme.wireDimensions.conversationBottomSheetItemSize,
    modifier: Modifier = Modifier
) {
    Icon(
        painter = painterResource(id = id),
        contentDescription = contentDescription,
        modifier = Modifier
            .size(size)
            .then(modifier)
    )
}

