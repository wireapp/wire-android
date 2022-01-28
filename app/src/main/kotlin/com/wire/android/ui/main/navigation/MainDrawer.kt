package com.wire.android.ui.main.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
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
import com.wire.android.ui.common.selectableBackground
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDrawer(scope: CoroutineScope, drawerState: DrawerState, navController: NavController) {
    val topItems = listOf(MainNavigationScreenItem.Conversations, MainNavigationScreenItem.Archive, MainNavigationScreenItem.Vault)
    val bottomItems = listOf(MainNavigationScreenItem.Settings, MainNavigationScreenItem.Support)

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
                    navigateToItem(navController, item, scope, drawerState)
                })
        }

        Spacer(modifier = Modifier.weight(1f))

        bottomItems.forEach { item ->
            DrawerItem(item = item,
                selected = currentRoute == item.route,
                onItemClick = {
                    navigateToItem(navController, item, scope, drawerState)
                })
        }
    }
}

@Composable
fun DrawerItem(item: MainNavigationScreenItem, selected: Boolean, onItemClick: (MainNavigationScreenItem) -> Unit) {
    val backgroundColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .fillMaxWidth()
            .height(40.dp)
            .background(backgroundColor)
            .selectableBackground(selected) { onItemClick(item) }
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
    DrawerItem(item = MainNavigationScreenItem.Conversations, selected = false, onItemClick = {})
}

@Preview(showBackground = false)
@Composable
fun DrawerItemSelectedPreview() {
    DrawerItem(item = MainNavigationScreenItem.Conversations, selected = true, onItemClick = {})
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun DrawerPreview() {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val navController = rememberNavController()
    MainDrawer(scope = scope, drawerState = drawerState, navController = navController)
}
