package com.wire.android.ui.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun WireBottomNavigationBar(
    items: List<WireBottomNavigationItemData>,
    navController: NavController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(MaterialTheme.wireDimensions.bottomNavigationHeight),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = MaterialTheme.wireDimensions.bottomNavigationHeight,
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            items.forEachIndexed { index, item ->
                val modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(
                        top = MaterialTheme.wireDimensions.bottomNavigationVerticalPadding,
                        bottom = MaterialTheme.wireDimensions.bottomNavigationVerticalPadding,
                        start = if (index == 0) MaterialTheme.wireDimensions.bottomNavigationHorizontalPadding
                        else MaterialTheme.wireDimensions.bottomNavigationBetweenItemsPadding,
                        end = if (index == items.lastIndex) MaterialTheme.wireDimensions.bottomNavigationHorizontalPadding
                        else MaterialTheme.wireDimensions.bottomNavigationBetweenItemsPadding
                    )

                WireBottomNavigationItem(
                    data = item,
                    selected = currentRoute == item.route,
                    modifier = modifier
                ) {
                    navController.navigate(item.route) {
                        popUpTo(0) {
                            saveState = true
                            inclusive = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.WireBottomNavigationItem(
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
    val route: String,
)
