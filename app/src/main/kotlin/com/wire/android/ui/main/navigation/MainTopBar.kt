package com.wire.android.ui.main.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.wire.android.R
import com.wire.android.ui.common.SearchBarCollapsed
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.theme.WireLightColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Composable
fun MainTopBar(scope: CoroutineScope, scaffoldState: ScaffoldState, navController: NavController, hasSearchBar: Boolean = false) {

    val currentNavigationScreenItem: MainNavigationScreenItem? = navController.getCurrentNavigationItem()
    val title = stringResource(currentNavigationScreenItem?.title ?: R.string.app_name)

    ConstraintLayout(Modifier.background(WireLightColors.background)) {
        val (topBar, searchBar) = createRefs()
        TopAppBar(
            modifier = Modifier.fillMaxWidth().constrainAs(topBar) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            },
            elevation = 0.dp,
            backgroundColor = WireLightColors.background,
            contentColor = WireLightColors.onBackground,
            content = {
                ConstraintLayout(Modifier.fillMaxSize()) {
                    val (menu, barTitle, avatar) = createRefs()
                    IconButton(
                        modifier = Modifier.constrainAs(menu) {
                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                            start.linkTo(parent.start)
                        },
                        onClick = {
                            scope.launch { scaffoldState.drawerState.open() }
                        }) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "",
                            tint = WireLightColors.onBackground
                        )
                    }
                    Text(
                        modifier = Modifier.constrainAs(barTitle) {
                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                            start.linkTo(menu.end)
                            end.linkTo(avatar.start)
                        },
                        text = title, fontSize = 18.sp
                    )
                    UserProfileAvatar(
                        modifier = Modifier.constrainAs(avatar) {
                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                            end.linkTo(parent.end)
                        },
                        avatarUrl = ""
                    ) {
                        navigateToItem(navController, MainNavigationScreenItem.UserProfile, scope, scaffoldState)
                    }
                }
            },
        )
        if (hasSearchBar) {
            SearchBarCollapsed(
                modifier = Modifier.constrainAs(searchBar) {
                    top.linkTo(topBar.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
                hintText = stringResource(R.string.search_bar_hint, title)
            )
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
