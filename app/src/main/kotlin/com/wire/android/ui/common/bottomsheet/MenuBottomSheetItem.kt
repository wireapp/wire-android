package com.wire.android.ui.common.bottomsheet

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.dimensions
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .defaultMinSize(minHeight = MaterialTheme.wireDimensions.conversationBottomSheetItemHeight)
            .fillMaxWidth()
            .clickable { onItemClick() }
            .padding(MaterialTheme.wireDimensions.conversationBottomSheetItemPadding)
    ) {
        icon()
        Spacer(modifier = Modifier.width(12.dp))
        MenuItemTitle(title = title)
        if (action != null) {
            Spacer(modifier = Modifier.width(MaterialTheme.wireDimensions.spacing12x))
            Spacer(modifier = Modifier.weight(1f))  // combining both in one modifier doesn't work
            action()
        }
    }
}

@Composable
fun buildMenuSheetItems(items: List<@Composable () -> Unit>) {
    items.forEach { itemBuilder ->
        Divider(thickness = 0.5.dp)
        itemBuilder()
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
    size: Dp = MaterialTheme.wireDimensions.wireIconButtonSize,
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

@Preview
@Composable
fun MenuBottomSheetItemPreview() {
    MenuBottomSheetItem(
        title = "very long looooooong title",
        icon = {
            MenuItemIcon(
                id = R.drawable.ic_erase,
                contentDescription = "",
            )
        },
        action = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "very long looooooong action",
                    style = MaterialTheme.wireTypography.body01,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier.weight(weight = 1f, fill = false)
                )
                Spacer(modifier = Modifier.size(dimensions().spacing16x))
                ArrowRightIcon()
            }
        }
    )
}
