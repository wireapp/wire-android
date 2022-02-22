package com.wire.android.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@ExperimentalMaterialApi
@ExperimentalMaterial3Api
@Composable
fun HomeScreen(startScreen: String?, viewModel: HomeViewModel) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val currentItem = HomeNavigationItem.getCurrentNavigationItem(navController)
    val scope = rememberCoroutineScope()

    val topBar: @Composable () -> Unit = {
        HomeTopBar(currentItem.title, currentItem.isSearchable, drawerState, scope, viewModel)
    }
    val drawerContent: @Composable ColumnScope.() -> Unit = {
        HomeDrawer(drawerState, currentItem.route, navController, HomeNavigationItem.all, scope, viewModel)
    }

    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }
    NavigationDrawer(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerTonalElevation = 0.dp,
        drawerShape = RectangleShape,
        drawerState = drawerState,
        drawerContent = drawerContent,
        gesturesEnabled = currentItem.isSwipeable
    ) {
        val homeState = rememberHomeState()
        val bottomSheetState = homeState.homeBottomSheetState
        if (bottomSheetState is HomeBottomSheetState.HasBottomSheet) {
            WireModalSheetLayout(
                sheetState = homeState.bottomSheetState,
                sheetContent = bottomSheetState.bottomSheetContent
            ) {
                Box {
                    Scaffold {
                        val startDestination = HomeNavigationItem.all.firstOrNull { startScreen == it.route }?.route
                        HomeNavigationGraph(navController = navController, startDestination = startDestination)
                    }
                    topBar()
                }
            }
        } else {
            Box {
                Scaffold {
                    val startDestination = HomeNavigationItem.all.firstOrNull { startScreen == it.route }?.route
                    HomeNavigationGraph(navController = navController, startDestination = startDestination)
                }
                // We are not including the topBar in the Scaffold to correctly handle the collapse scroll effect on the search,
                // which will not be possible when using Scaffold topBar argument
                topBar()
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
class HomeState(
    private val coroutineScope: CoroutineScope,
    val bottomSheetState: ModalBottomSheetState,
    defaultHomeBottomSheetState: HomeBottomSheetState
) {

    var homeBottomSheetState by mutableStateOf(defaultHomeBottomSheetState)
        private set

    fun expandBottomSheet() {
        if (homeBottomSheetState is HomeBottomSheetState.HasBottomSheet) {
            coroutineScope.launch { bottomSheetState.animateTo(ModalBottomSheetValue.Expanded) }
        }
    }

    fun changeBottomSheetContent(content: @Composable ColumnScope.() -> Unit) {
        homeBottomSheetState = HomeBottomSheetState.HasBottomSheet(content)
    }

}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberHomeState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    bottomSheetState: ModalBottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Expanded),
    homeBottomSheetState: HomeBottomSheetState = HomeBottomSheetState.NoBottomSheet
): HomeState {
    return remember {
        HomeState(
            coroutineScope,
            bottomSheetState,
            homeBottomSheetState
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
sealed class HomeBottomSheetState {
    object NoBottomSheet : HomeBottomSheetState()
    class HasBottomSheet(
        val bottomSheetContent: @Composable ColumnScope.() -> Unit
    ) : HomeBottomSheetState()
}

val LocalHomeState = compositionLocalOf<HomeState?> { null }
