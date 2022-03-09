package com.wire.android.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.wire.android.navigation.HomeNavigationGraph
import com.wire.android.navigation.HomeNavigationItem
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterialApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun HomeScreen(startScreen: String?, viewModel: HomeViewModel) {
    val navController = rememberAnimatedNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val currentItem = HomeNavigationItem.getCurrentNavigationItem(navController)
    val scope = rememberCoroutineScope()

    val topBar: @Composable () -> Unit = {
        HomeTopBar(currentItem.title, currentItem.isSearchable, drawerState, scope, viewModel)
    }
    val drawerContent: @Composable ColumnScope.() -> Unit = {
        HomeDrawer(drawerState, currentItem.route, navController, HomeNavigationItem.all, scope, viewModel)
    }

    NavigationDrawer(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerTonalElevation = 0.dp,
        drawerShape = RectangleShape,
        drawerState = drawerState,
        drawerContent = drawerContent,
        gesturesEnabled = drawerState.isOpen
    ) {
        val homeState = rememberHomeState()

        val homeContent: @Composable () -> Unit = {
            Box {
                val startDestination = HomeNavigationItem.all.firstOrNull { startScreen == it.route }?.route
                HomeNavigationGraph(
                    homeState = homeState,
                    navController = navController,
                    startDestination = startDestination
                )
                // We are not including the topBar in the Scaffold to correctly handle the collapse scroll effect on the search,
                // which will not be possible when using Scaffold topBar argument
                topBar()
            }
        }

        val homeScreen: @Composable () -> Unit = homeState.homeBottomSheetContent?.run {
            {
                WireModalSheetLayout(
                    sheetState = homeState.bottomSheetState,
                    sheetContent = this
                ) {
                    homeContent()
                }
            }
        } ?: { homeContent() }

        homeScreen()
    }
    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }
}

@OptIn(ExperimentalMaterialApi::class)
class HomeState(
    private val coroutineScope: CoroutineScope,
    val bottomSheetState: ModalBottomSheetState,
    bottomSheetContent: @Composable (ColumnScope.() -> Unit)?
) {

    var homeBottomSheetContent by mutableStateOf(bottomSheetContent)
        private set

    fun expandBottomSheet() {
        coroutineScope.launch { bottomSheetState.animateTo(ModalBottomSheetValue.Expanded) }
    }

    fun changeBottomSheetContent(content: @Composable ColumnScope.() -> Unit) {
        homeBottomSheetContent = content
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberHomeState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    bottomSheetState: ModalBottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden),
    bottomSheetContent: @Composable (ColumnScope.() -> Unit)? = null
): HomeState {
    return remember {
        HomeState(
            coroutineScope,
            bottomSheetState,
            bottomSheetContent
        )
    }
}
