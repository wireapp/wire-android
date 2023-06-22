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

package com.wire.android.ui.common

import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.ramcosta.composedestinations.navigation.navigate
import com.ramcosta.composedestinations.spec.Direction
import com.wire.android.ui.NavGraphs
import com.wire.android.ui.appCurrentDestinationAsState
import com.wire.android.ui.bottombar.BottomBarDestination
import com.wire.android.ui.destinations.Destination
import com.wire.android.ui.startAppDestination
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WireBottomNavigationBar(
//    items: List<WireBottomNavigationItemData>,
    navController: NavController,
    onBottomBarItemClick: (Direction) -> Unit
) {
    val currentDestination: Destination = navController.appCurrentDestinationAsState().value
        ?: NavGraphs.home.startAppDestination

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(MaterialTheme.wireDimensions.bottomNavigationHeight),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = MaterialTheme.wireDimensions.bottomNavigationShadowElevation,
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            BottomBarDestination.values().forEachIndexed { index, item ->
                val modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(
                        top = MaterialTheme.wireDimensions.bottomNavigationVerticalPadding,
                        bottom = MaterialTheme.wireDimensions.bottomNavigationVerticalPadding,
                        start = if (index == 0) MaterialTheme.wireDimensions.bottomNavigationHorizontalPadding
                        else MaterialTheme.wireDimensions.bottomNavigationBetweenItemsPadding,
                        end = if (index == BottomBarDestination.values().lastIndex) MaterialTheme.wireDimensions.bottomNavigationHorizontalPadding
                        else MaterialTheme.wireDimensions.bottomNavigationBetweenItemsPadding
                    )


                    Log.d("navigation", "WireBottomNavigationBar: route = ${item.direction}")
                    WireBottomNavigationItem(
                        data = WireBottomNavigationItemData(
                            icon = item.icon,
                            title = item.label,
                            notificationAmount = 0,
                            direction = item.direction
                        ),
                        selected = currentDestination == item.direction,
                        modifier = modifier
                    ) {
                        Log.d("navigation", "WireBottomNavigationBar: clicked navController = $navController")
                        Log.d("navigation", "WireBottomNavigationBar: clicked item = $it")
                        onBottomBarItemClick(it.direction)
                    }

            }
        }
    }
}

@Composable
fun WireBottomNavigationItem(
    data: WireBottomNavigationItemData,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onItemClick: (WireBottomNavigationItemData) -> Unit
) {
    val backgroundColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground
    Box(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .selectableBackground(selected) { onItemClick(data) }
            .background(backgroundColor)
            .padding(MaterialTheme.wireDimensions.bottomNavigationItemPadding)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Image(
                painter = painterResource(id = data.icon),
                contentDescription = stringResource(data.title),
                colorFilter = ColorFilter.tint(contentColor),
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .width(18.dp)
                    .height(18.dp)
            )
            Text(
                text = stringResource(id = data.title),
                style = MaterialTheme.wireTypography.button05,
                color = contentColor,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 4.dp)
            )
        }

        if (data.notificationAmount > 0) {
            Box(
                modifier = Modifier
                    .padding(end = 20.dp)
                    .align(Alignment.TopEnd)
            ) {

                Text(
                    text = data.notificationAmount.toString(),
                    color = MaterialTheme.colorScheme.onError,
                    style = MaterialTheme.wireTypography.badge01,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.error)
                        .align(Alignment.Center)
                        .padding(top = 2.dp, bottom = 2.dp, start = 4.dp, end = 4.dp)
                        .defaultMinSize(minWidth = 12.dp)
                )
            }
        }
    }
}

data class WireBottomNavigationItemData(
    @DrawableRes val icon: Int,
    @StringRes val title: Int,
    val notificationAmount: Int,
    val direction: Direction,
)
