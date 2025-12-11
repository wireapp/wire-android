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

package com.wire.android.ui.home.drawer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.wire.android.R
import com.wire.android.ui.common.R as commonR
import com.wire.android.navigation.ExternalDirectionLess
import com.wire.android.navigation.ExternalUriDirection
import com.wire.android.navigation.ExternalUriStringResDirection
import com.wire.android.navigation.HomeDestination
import com.wire.android.ui.common.Logo
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.selectableBackground
import com.wire.android.ui.common.spacers.HorizontalSpace
import com.wire.android.ui.home.conversationslist.common.UnreadMessageEventBadge
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun HomeDrawer(
    homeDrawerState: HomeDrawerState,
    currentRoute: String?,
    navigateToHomeItem: (HomeDestination) -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(
                start = MaterialTheme.wireDimensions.homeDrawerHorizontalPadding,
                end = MaterialTheme.wireDimensions.homeDrawerHorizontalPadding,
                bottom = MaterialTheme.wireDimensions.homeDrawerBottomPadding
            )

    ) {
        Logo(
            modifier = Modifier
                .padding(
                    horizontal = MaterialTheme.wireDimensions.homeDrawerLogoHorizontalPadding,
                    vertical = MaterialTheme.wireDimensions.homeDrawerLogoVerticalPadding
                )
                .width(MaterialTheme.wireDimensions.homeDrawerLogoWidth)
                .height(MaterialTheme.wireDimensions.homeDrawerLogoHeight)
        )

        val (topItems, bottomItems) = homeDrawerState.items
        topItems.forEach { item ->
            MapToDrawerItem(navigateToHomeItem, onCloseDrawer, currentRoute, item)
        }

        Spacer(modifier = Modifier.weight(1f))

        bottomItems.forEach { item ->
            MapToDrawerItem(navigateToHomeItem, onCloseDrawer, currentRoute, item)
        }
    }
}

@Composable
fun MapToDrawerItem(
    navigateToHomeItem: (HomeDestination) -> Unit,
    onCloseDrawer: () -> Unit,
    currentRoute: String?,
    drawerUiItem: DrawerUiItem
) {
    val context = LocalContext.current
    fun navigateAndCloseDrawer(item: HomeDestination) {
        navigateToHomeItem(item)
        onCloseDrawer()
    }

    with(drawerUiItem) {
        when (this) {
            is DrawerUiItem.DynamicExternalNavigationItem -> DrawerItem(
                destination = destination,
                selected = currentRoute == destination.direction.route,
                onItemClick = remember {
                    {
                        com.wire.android.util.CustomTabsHelper.launchUrl(context, url)
                        onCloseDrawer()
                    }
                }
            )

            is DrawerUiItem.RegularItem -> DrawerItem(
                destination = destination,
                selected = currentRoute == destination.direction.route,
                onItemClick = remember { { navigateAndCloseDrawer(destination) } }
            )

            is DrawerUiItem.UnreadCounterItem -> DrawerItem(
                destination = destination,
                unreadCount = this.unreadCount.toInt(),
                selected = currentRoute == destination.direction.route,
                onItemClick = remember { { navigateAndCloseDrawer(destination) } }
            )
        }
    }
}

@Composable
fun DrawerItem(
    destination: HomeDestination,
    selected: Boolean,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier,
    unreadCount: Int = 0,
) {
    val backgroundColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(bottom = dimensions().spacing8x)
            .clip(RoundedCornerShape(dimensions().spacing12x))
            .fillMaxWidth()
            .height(dimensions().spacing40x)
            .background(backgroundColor)
            .selectableBackground(selected, stringResource(R.string.content_description_open_label), onItemClick),
    ) {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier.width(dimensions().spacing48x)
        ) {
            Image(
                painter = painterResource(id = destination.icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(contentColor),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .height(dimensions().spacing16x)
                    .padding(start = dimensions().spacing16x)
            )
        }
        Text(
            style = MaterialTheme.wireTypography.button02,
            text = destination.title.asString(),
            textAlign = TextAlign.Start,
            color = contentColor,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .weight(1F)
        )
        UnreadMessageEventBadge(unreadMessageCount = unreadCount)
        with(destination) {
            if (direction is ExternalUriDirection || direction is ExternalUriStringResDirection || direction is ExternalDirectionLess) {
                HorizontalSpace.x8()
                Icon(
                    painter = painterResource(commonR.drawable.ic_open_in_new),
                    contentDescription = null,
                    tint = colorsScheme().secondaryText,
                    modifier = Modifier.size(dimensions().spacing16x)
                )
            }
        }
        HorizontalSpace.x12()
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSelectedArchivedItemWithUnreadCount() = WireTheme {
    Box(modifier = Modifier.background(color = MaterialTheme.colorScheme.surface)) {
        DrawerItem(
            destination = HomeDestination.Archive,
            selected = true,
            onItemClick = {},
            unreadCount = 100
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewUnSelectedArchivedItemWithUnreadCount() = WireTheme {
    Box(modifier = Modifier.background(color = MaterialTheme.colorScheme.surface)) {
        DrawerItem(
            destination = HomeDestination.Archive,
            selected = false,
            onItemClick = {},
            unreadCount = 100
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewItemWithExternalDestination() = WireTheme {
    Box(modifier = Modifier.background(color = MaterialTheme.colorScheme.surface)) {
        DrawerItem(
            destination = HomeDestination.Support,
            selected = false,
            onItemClick = {},
        )
    }
}
