/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.navigation.HomeDestination
import com.wire.android.ui.common.Logo
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.selectableBackground
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun HomeDrawer(
    currentRoute: String?,
    navigateToHomeItem: (HomeDestination) -> Unit,
    onCloseDrawer: () -> Unit,
) {
    val context = LocalContext.current

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

        val topItems = listOf(
            HomeDestination.Conversations,
            HomeDestination.Archive
        )
        // TODO: Re-enable once we have Archive & Vault
        // listOf(HomeDestination.Conversations, HomeDestination.Archive, HomeDestination.Vault)
        topItems.forEach { item ->
            DrawerItem(
                destination = item,
                selected = currentRoute == item.direction.route,
                onItemClick = remember { { navigateAndCloseDrawer(item) } }
            )
        }

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
fun DrawerItem(destination: HomeDestination, selected: Boolean, onItemClick: () -> Unit) {
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
            color = contentColor,
            modifier = Modifier
                .align(Alignment.CenterVertically)
        )
    }
}
