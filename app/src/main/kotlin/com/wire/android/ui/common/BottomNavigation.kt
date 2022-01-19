package com.wire.android.ui.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wire.android.ui.main.convesations.ConversationsNavigationItem
import com.wire.android.ui.theme.WireColor
import com.wire.android.ui.theme.WireLightColors

@Composable
fun WireBottomNavigationBar(items: List<WireBottomNavigationItemData>, navController: NavController, spaceBetweenItems: Dp = 18.dp) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    Column {
        Spacer(
            Modifier
                .background(WireColor.LightShadow)
                .fillMaxWidth()
                .height(1.dp)
        )

        Row(
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth()
                .background(WireColor.LightBackgroundWhite)
                .height(58.dp)
        ) {
            items.forEachIndexed { index, item ->
                WireBottomNavigationItem(item, currentRoute == item.route) {
                    navController.navigate(item.route) {
                        popUpTo(0) {
                            saveState = true
                            inclusive = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }

                if (index < items.size - 1) {
                    Spacer(modifier = Modifier.width(spaceBetweenItems))
                }
            }
        }

    }
}

@Composable
fun RowScope.WireBottomNavigationItem(
    data: WireBottomNavigationItemData,
    selected: Boolean,
    onItemClick: (WireBottomNavigationItemData) -> Unit
) {
    val backgroundColor = if (selected) WireLightColors.secondary.copy(0.12f) else Color.Transparent
    val contentColor = if (selected) WireLightColors.secondary else WireLightColors.onBackground
    Box(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .selectableBackground(selected) { onItemClick(data) }
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .padding(6.dp)
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
                fontSize = 12.sp,
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
                    color = WireColor.LightTextWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(WireColor.LightRed)
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

@Preview(showBackground = false)
@Composable
fun WireBottomNavigationBarPreview() {
    val navController = rememberNavController()
    val items = ConversationsNavigationItem.values()
        .map { it.intoBottomNavigationItemData(12) }
    WireBottomNavigationBar(items, navController)
}
