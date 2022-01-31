package com.wire.android.ui.main.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.wire.android.R
import com.wire.android.ui.common.SearchBarUI
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.theme.wireTypography
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Composable
fun MainTopBar(scope: CoroutineScope, drawerState: DrawerState, navController: NavController, hasSearchBar: Boolean = false) {

    val currentNavigationScreenItem: MainNavigationScreenItem? = navController.getCurrentNavigationItem()
    val title = stringResource(currentNavigationScreenItem?.title ?: R.string.app_name)

    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
        CenterAlignedTopAppBar(
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground
            ),
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.wireTypography.title01
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = { scope.launch { drawerState.open() } }) {
                    Icon(imageVector = Icons.Filled.Menu, contentDescription = "")
                }
            },
            actions = {
                UserProfileAvatar(avatarUrl = "") {
                    navigateToItem(navController, MainNavigationScreenItem.UserProfile, scope, drawerState)
                }
            },
        )
        if (hasSearchBar) {
            SearchBarUI(placeholderText = stringResource(R.string.search_bar_hint, title.lowercase()))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Preview(showBackground = false)
@Composable
fun MainTopBarPreview() {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val navController = rememberNavController()
    MainTopBar(scope = scope, drawerState = drawerState, navController = navController)
}
