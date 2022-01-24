package com.wire.android.ui.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomNavigation
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.wire.android.ui.theme.Dimensions
import com.wire.android.ui.theme.button5

@Composable
fun WireBottomNavigationBar(
    items: List<WireBottomNavigationItemData>,
    navController: NavController,
    spaceBetweenItems: Dp = 16.dp
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    BottomNavigation(backgroundColor = MaterialTheme.colors.surface) {
        items.forEachIndexed { index, item ->
            val modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colors.surface)
                .padding(
                    top = Dimensions.bottomNavigationPadding,
                    bottom = Dimensions.bottomNavigationPadding,
                    start = Dimensions.bottomNavigationPadding + if (index == 0) 0.dp else spaceBetweenItems / 2,
                    end = Dimensions.bottomNavigationPadding + if (index == items.lastIndex) 0.dp else spaceBetweenItems / 2
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

@Composable
fun RowScope.WireBottomNavigationItem(
    data: WireBottomNavigationItemData,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onItemClick: (WireBottomNavigationItemData) -> Unit
) {
    val backgroundColor = if (selected) MaterialTheme.colors.secondary.copy(0.12f) else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colors.secondary else MaterialTheme.colors.onBackground
    Box(
        modifier
            .selectableBackground(selected) { onItemClick(data) }
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .padding(Dimensions.bottomNavigationItemPadding)
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
                style = MaterialTheme.typography.button5,
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
                    fontSize = 10.sp,
                    color = MaterialTheme.colors.onSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colors.error)
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
    val content: @Composable (NavBackStackEntry) -> Unit
)
