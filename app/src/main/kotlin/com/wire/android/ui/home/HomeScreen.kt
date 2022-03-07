package com.wire.android.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wire.android.R
import com.wire.android.model.UserStatus
import com.wire.android.navigation.HomeNavigationGraph
import com.wire.android.navigation.HomeNavigationItem
import com.wire.android.ui.common.NavigationIconType
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.newconversation.AppTopBarWithSearchBarLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@ExperimentalMaterialApi
@ExperimentalMaterial3Api
@Composable
fun HomeScreen(startScreen: String?, viewModel: HomeViewModel) {
    val homeState = rememberHomeState()

    with(homeState) {
        val drawerContent: @Composable ColumnScope.() -> Unit = {
            HomeDrawer(
                drawerState = drawerState,
                currentRoute = currentNavigationItem.route,
                homeNavController = navController,
                topItems = HomeNavigationItem.all,
                scope = coroutineScope,
                viewModel = viewModel
            )
        }

        NavigationDrawer(
            drawerContainerColor = MaterialTheme.colorScheme.surface,
            drawerTonalElevation = 0.dp,
            drawerShape = RectangleShape,
            drawerState = drawerState,
            drawerContent = drawerContent,
            gesturesEnabled = currentNavigationItem.isSwipeable
        ) {
            val homeContent: @Composable () -> Unit = {
                with(currentNavigationItem) {
                    AppTopBarWithSearchBarLayout(
                        scrollPosition = scrollPosition,
                        searchBarHint = stringResource(R.string.label_search_people),
                        searchQuery = "",
                        onSearchQueryChanged = {},
                        onSearchClicked = { },
                        onCloseSearchClicked = {},
                        appTopBar = {
                            WireCenterAlignedTopAppBar(
                                title = stringResource(id = title),
                                onNavigationPressed = { openDrawer() },
                                navigationIconType = NavigationIconType.Menu,
                                actions = {
                                    UserProfileAvatar(avatarUrl = "", status = UserStatus.AVAILABLE) {
                                        viewModel.navigateToUserProfile()
                                    }
                                }
                            )
                        },
                        content = {
                            val startDestination = HomeNavigationItem.all.firstOrNull { startScreen == it.route }?.route

                            HomeNavigationGraph(
                                homeState = homeState,
                                navController = navController,
                                startDestination = startDestination
                            )
                        }
                    )

//                        if (isSearchable) {
//                            DeprecatedSearchTopBar(
//                                topBarTitle = stringResource(id = title),
//                                scrollPosition = scrollPosition,
//                                onNavigationPressed = { openDrawer() },
//                                navigationIconType = NavigationIconType.Menu,
//                                searchBar = {
//                                    SearchBar(
//                                        placeholderText = stringResource(
//                                            R.string.search_bar_hint,
//                                            stringResource(id = title).lowercase()
//                                        ),
//                                        modifier = Modifier.background(MaterialTheme.colorScheme.background)
//                                    )
//                                },
//                                actions = {
//                                    UserProfileAvatar(avatarUrl = "", status = UserStatus.AVAILABLE) {
//                                        viewModel.navigateToUserProfile()
//                                    }
//                                }
//                            )
//                        } else {
//                            WireCenterAlignedTopAppBar(
//                                title = stringResource(id = title),
//                                onNavigationPressed = { openDrawer() },
//                                navigationIconType = NavigationIconType.Menu,
//                                actions = {
//                                    UserProfileAvatar(avatarUrl = "", status = UserStatus.AVAILABLE) {
//                                        viewModel.navigateToUserProfile()
//                                    }
//                                }
//                            )
//                        }
                }
            }

            val homeBottomSheetContent = homeBottomSheetContent

            if (homeBottomSheetContent != null) {
                WireModalSheetLayout(
                    sheetState = bottomSheetState,
                    sheetContent = homeBottomSheetContent
                ) {
                    homeContent()
                }
            } else {
                homeContent()
            }
        }

        BackHandler(enabled = drawerState.isOpen) { closeDrawer() }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
class HomeState(
    val coroutineScope: CoroutineScope,
    val navController: NavHostController,
    val drawerState: DrawerState,
    val bottomSheetState: ModalBottomSheetState,
    bottomSheetContent: @Composable (ColumnScope.() -> Unit)?,
    val currentNavigationItem: HomeNavigationItem
) {

    var scrollPosition by mutableStateOf(0)
        private set

    var homeBottomSheetContent by mutableStateOf(bottomSheetContent)
        private set

    fun expandBottomSheet() {
        coroutineScope.launch { bottomSheetState.animateTo(ModalBottomSheetValue.Expanded) }
    }

    fun changeBottomSheetContent(content: @Composable ColumnScope.() -> Unit) {
        homeBottomSheetContent = content
    }

    fun updateScrollPosition(newScrollPosition: Int) {
        scrollPosition = newScrollPosition
    }

    fun closeDrawer() {
        coroutineScope.launch {
            drawerState.close()
        }
    }

    fun openDrawer() {
        coroutineScope.launch {
            drawerState.open()
        }
    }

}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun rememberHomeState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    navController: NavHostController = rememberNavController(),
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    bottomSheetState: ModalBottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden),
    bottomSheetContent: @Composable (ColumnScope.() -> Unit)? = null
): HomeState {
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val navigationItem = when (navBackStackEntry?.destination?.route) {
        HomeNavigationItem.Archive.route -> HomeNavigationItem.Archive
        HomeNavigationItem.Vault.route -> HomeNavigationItem.Vault
        else -> HomeNavigationItem.Conversations
    }

    val homeState = remember(
        navigationItem
    ) {
        HomeState(
            coroutineScope,
            navController,
            drawerState,
            bottomSheetState,
            bottomSheetContent,
            navigationItem
        )
    }

    return homeState
}
