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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wire.android.navigation.HomeDestination
import com.wire.android.ui.common.Logo
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.selectableBackground
import com.wire.android.ui.common.spacers.HorizontalSpace
import com.wire.android.ui.home.conversationslist.common.UnreadMessageEventBadge
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun HomeDrawer(
    homeDrawerState: HomeDrawerState,
    currentRoute: String?,
    navigateToHomeItem: (HomeDestination) -> Unit,
    onCloseDrawer: () -> Unit,
) {
    Column(
        modifier = Modifier
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

        fun navigateAndCloseDrawer(item: HomeDestination) {
            navigateToHomeItem(item)
            onCloseDrawer()
        }

        DrawerItem(
            destination = HomeDestination.Conversations,
            selected = currentRoute == HomeDestination.Conversations.direction.route,
            onItemClick = remember { { navigateAndCloseDrawer(HomeDestination.Conversations) } }
        )

        DrawerItem(
            destination = HomeDestination.Archive,
            unreadCount = homeDrawerState.unreadArchiveConversationsCount,
            selected = currentRoute == HomeDestination.Archive.direction.route,
            onItemClick = remember { { navigateAndCloseDrawer(HomeDestination.Archive) } }
        )

        Spacer(modifier = Modifier.weight(1f))

        val bottomItems = listOf(HomeDestination.WhatsNew, HomeDestination.Settings, HomeDestination.Support)
        bottomItems.forEach { item ->
            DrawerItem(
                destination = item,
                selected = currentRoute == item.direction.route,
                onItemClick = remember { { navigateAndCloseDrawer(item) } }
            )
        }
    }
}

@Composable
fun DrawerItem(destination: HomeDestination, selected: Boolean, unreadCount: Int = 0, onItemClick: () -> Unit) {
    val backgroundColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .fillMaxWidth()
            .height(40.dp)
            .background(backgroundColor)
            .selectableBackground(selected) { onItemClick() },
    ) {
        Image(
            painter = painterResource(id = destination.icon),
            contentDescription = stringResource(destination.title),
            colorFilter = ColorFilter.tint(contentColor),
            contentScale = ContentScale.Fit,
            modifier = Modifier.padding(start = dimensions().spacing16x, end = dimensions().spacing16x)
        )
        Text(
            style = MaterialTheme.wireTypography.button02,
            text = stringResource(id = destination.title),
            textAlign = TextAlign.Start,
            color = contentColor,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .weight(1F)
        )
        UnreadMessageEventBadge(unreadMessageCount = unreadCount)
        HorizontalSpace.x12()
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSelectedArchivedItemWithUnreadCount() {
    Box(modifier = Modifier.background(color = MaterialTheme.colorScheme.surface)) {
        DrawerItem(
            destination = HomeDestination.Archive,
            selected = true,
            unreadCount = 100,
            {}
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewUnSelectedArchivedItemWithUnreadCount() {
    Box(modifier = Modifier.background(color = MaterialTheme.colorScheme.surface)) {
        DrawerItem(
            destination = HomeDestination.Archive,
            selected = false,
            unreadCount = 100,
            {}
        )
    }
}
