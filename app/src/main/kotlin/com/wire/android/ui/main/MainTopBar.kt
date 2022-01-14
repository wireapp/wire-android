package com.wire.android.ui.main

import androidx.compose.material.DrawerValue
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.rememberDrawerState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wire.android.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun MainTopBar(scope: CoroutineScope, scaffoldState: ScaffoldState, navController: NavController) {

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val title = stringResource(MainScreen.fromRout(currentRoute)?.title ?: R.string.app_name)

    TopAppBar(
        title = { Text(text = title, fontSize = 18.sp) },
        navigationIcon = {
            IconButton(onClick = {
                scope.launch { scaffoldState.drawerState.open() }
            }) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "",
                    tint = colorResource(id = R.color.text_black)
                )
            }
        },
        backgroundColor = colorResource(id = R.color.background_grey),
        contentColor = colorResource(id = R.color.text_black)
    )
}

@Preview(showBackground = false)
@Composable
fun MainTopBarPreview() {
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
    val navController = rememberNavController()
    MainTopBar(scope = scope, scaffoldState = scaffoldState, navController = navController)
}
