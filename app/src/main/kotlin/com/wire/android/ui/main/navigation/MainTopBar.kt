package com.wire.android.ui.main.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.DrawerValue
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.rememberDrawerState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.wire.android.R
import com.wire.android.ui.common.SearchBarUI
import com.wire.android.ui.common.UserProfileAvatar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Composable
fun MainTopBar(scope: CoroutineScope, scaffoldState: ScaffoldState, navController: NavController, hasSearchBar: Boolean = false) {

    val currentNavigationScreenItem: MainNavigationScreenItem? = navController.getCurrentNavigationItem()
    val title = stringResource(currentNavigationScreenItem?.title ?: R.string.app_name)

    Column(Modifier.fillMaxWidth().background(MaterialTheme.colors.background)) {
        TopAppBar(
            modifier = Modifier.fillMaxWidth(),
            elevation = 0.dp,
            backgroundColor = MaterialTheme.colors.background,
            contentColor = MaterialTheme.colors.onBackground,
            content = {
                IconButton(
                    onClick = {
                        scope.launch { scaffoldState.drawerState.open() }
                    }) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "",
                        tint = MaterialTheme.colors.onBackground
                    )
                }
                Text(modifier = Modifier.weight(weight = 1f), textAlign = TextAlign.Center, text = title, fontSize = 18.sp)
                UserProfileAvatar(avatarUrl = "") {
                    navigateToItem(navController, MainNavigationScreenItem.UserProfile, scope, scaffoldState)
                }
            },
        )
        if (hasSearchBar) {
            SearchBarUI(placeholderText = stringResource(R.string.search_bar_hint, title))
        }
    }
}

@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Preview(showBackground = false)
@Composable
fun MainTopBarPreview() {
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
    val navController = rememberNavController()
    MainTopBar(scope = scope, scaffoldState = scaffoldState, navController = navController)
}
