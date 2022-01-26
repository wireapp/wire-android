package com.wire.android.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.*
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.wire.android.navigation.*
import com.wire.android.ui.drawer.WireDrawer
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.topbar.WireTopBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@AndroidEntryPoint
class WireActivity : AppCompatActivity() {

    @Inject
    lateinit var navigationManager: NavigationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WireTheme {

                val navController = rememberNavController()
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val currentItem = navController.getCurrentNavigationItem()
                val scope = rememberCoroutineScope()

                setUpNavigation(drawerState, navController, scope)

                val topBar = WireTopBar(navigationElements = currentItem?.navigationElements)
                val drawerContent = WireDrawer(currentItem?.route, currentItem?.navigationElements)

                if (drawerContent == null) {
                    Scaffold(
                        topBar = topBar,
                    ) {
                        NavigationGraph(navController = navController)
                    }
                } else {
                    NavigationDrawer(
                        drawerContainerColor = Color.White,
                        drawerTonalElevation = 0.dp,
                        drawerShape = RectangleShape,
                        drawerState = drawerState,
                        drawerContent = drawerContent
                    ) {
                        Scaffold(
                            topBar = topBar,
                        ) {
                            NavigationGraph(navController = navController)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun setUpNavigation(
        drawerState: DrawerState,
        navController: NavHostController,
        scope: CoroutineScope
    ) {
        LaunchedEffect(drawerState) {

            navigationManager.navigateState
                .onEach { command ->
                    if (command == null) return@onEach

                    navigateToItem(navController, command)
                }
                .launchIn(scope)

            navigationManager.navigateBack
                .onEach { navController.popBackStack() }
                .launchIn(scope)

            navigationManager.drawerState
                .onEach { isOpened ->
                    scope.launch {
                        if (isOpened) drawerState.open()
                        else drawerState.close()
                    }
                }
                .launchIn(scope)
        }
    }

}

