package com.wire.android.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DrawerValue
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.material.rememberDrawerState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wire.android.ui.common.Logo
import com.wire.android.ui.theme.WireLightColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun Drawer(scope: CoroutineScope, scaffoldState: ScaffoldState, navController: NavController) {

    val topItems = listOf(MainScreen.Conversations, MainScreen.Archive, MainScreen.Vault)
    val bottomItems = listOf(MainScreen.Settings, MainScreen.Support)

    Column(
        modifier = Modifier
            .padding(top = 40.dp, start = 8.dp, end = 8.dp, bottom = 16.dp)

    ) {
        Logo()

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
        )

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        topItems.forEach { item ->
            DrawerItem(item = item,
                selected = currentRoute == item.route,
                onItemClick = {
                    itemClickActions(navController, item, scope, scaffoldState)
                })
        }

        Spacer(modifier = Modifier.weight(1f))

        bottomItems.forEach { item ->
            DrawerItem(item = item,
                selected = currentRoute == item.route,
                onItemClick = {
                    itemClickActions(navController, item, scope, scaffoldState)
                })
        }

    }
}

private fun itemClickActions(
    navController: NavController,
    item: MainScreen,
    scope: CoroutineScope,
    scaffoldState: ScaffoldState
) {
    navController.navigate(item.route) {
        navController.graph.startDestinationRoute?.let { route ->
            popUpTo(route) {
                saveState = true
            }
        }
        launchSingleTop = true
        restoreState = true
    }

    scope.launch { scaffoldState.drawerState.close() }
}

@Composable
fun DrawerItem(item: MainScreen, selected: Boolean, onItemClick: (MainScreen) -> Unit) {
    val backgroundColor = if (selected) WireLightColors.secondary else Color.Transparent
    val contentColor = if (selected) WireLightColors.onSecondary else WireLightColors.onBackground
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .fillMaxWidth()
            .clickable(onClick = { onItemClick(item) })
            .height(40.dp)
            .background(backgroundColor)
    ) {
        Image(
            painter = painterResource(id = item.icon),
            contentDescription = stringResource(item.title),
            colorFilter = ColorFilter.tint(contentColor),
            contentScale = ContentScale.Fit,
            modifier = Modifier.padding(16.dp)
        )
        Text(
            text = stringResource(id = item.title),
            fontSize = 14.sp,
            color = contentColor,
            modifier = Modifier
                .align(Alignment.CenterVertically)
        )
    }
}

@Preview(showBackground = false)
@Composable
fun DrawerItemPreview() {
    DrawerItem(item = MainScreen.Conversations, selected = false, onItemClick = {})
}

@Preview(showBackground = false)
@Composable
fun DrawerItemSelectedPreview() {
    DrawerItem(item = MainScreen.Conversations, selected = true, onItemClick = {})
}

@Preview(showBackground = true)
@Composable
fun DrawerPreview() {
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
    val navController = rememberNavController()
    Drawer(scope = scope, scaffoldState = scaffoldState, navController = navController)
}
