/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.ui.common.bottomsheet

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.model.ClickBlockParams
import com.wire.android.model.Clickable
import com.wire.android.ui.common.clickable
import com.wire.android.ui.common.divider.WireDivider
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import io.github.esentsov.PackagePrivate

@Composable
fun MenuBottomSheetItem(
    title: String,
    icon: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    clickBlockParams: ClickBlockParams = ClickBlockParams(),
    itemProvidedColor: Color = MaterialTheme.colorScheme.secondary,
    onItemClick: () -> Unit = {},
    enabled: Boolean = true,
) {
    CompositionLocalProvider(LocalContentColor provides itemProvidedColor) {
        val clickable = remember(onItemClick, clickBlockParams) {
            Clickable(
                clickBlockParams = clickBlockParams,
                onClick = onItemClick,
                enabled = enabled
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .defaultMinSize(minHeight = MaterialTheme.wireDimensions.conversationBottomSheetItemHeight)
                .fillMaxWidth()
                .clickable(clickable)
                .padding(MaterialTheme.wireDimensions.conversationBottomSheetItemPadding)
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(12.dp))
            }
            MenuItemTitle(title = title)
            if (action != null) {
                Spacer(modifier = Modifier.width(MaterialTheme.wireDimensions.spacing12x))
                Spacer(modifier = Modifier.weight(1f)) // combining both in one modifier doesn't work
                action()
            }
        }
    }
}

@Composable
fun buildMenuSheetItems(items: List<@Composable () -> Unit>) {
    items.forEach { itemBuilder ->
        // Make sure that every item added to this list is actually not empty. Otherwise, the divider will be still drawn and give the
        // impression that it has extra thickness
        itemBuilder()
        WireDivider()
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
