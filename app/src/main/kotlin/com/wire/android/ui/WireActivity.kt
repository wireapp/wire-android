package com.wire.android.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
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
                val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
                val currentItem = navController.getCurrentNavigationItem()
                val scope = rememberCoroutineScope()

                println("cyka setting up 0 ")

                setUpNavigation(scaffoldState, navController, scope)

                val topBar: @Composable () -> Unit = { WireTopBar(navigationType = currentItem?.type) }
                val drawerContent = WireDrawer(currentItem?.route, currentItem?.type)

                Scaffold(
                    scaffoldState = scaffoldState,
                    topBar = topBar,
                    drawerContent = drawerContent
                ) {
                    NavigationGraph(navController = navController)
                }
            }
        }
    }

    @Composable
    private fun setUpNavigation(
        scaffoldState: ScaffoldState,
        navController: NavHostController,
        scope: CoroutineScope
    ) {
        LaunchedEffect(scaffoldState) {
            println("cyka setting up 1")

            navigationManager.navigateState
                .onEach { command ->
                    println("cyka to ${command?.route} ")

                    if (command == null) return@onEach

                    navigateToItem(navController, command)
                }
                .launchIn(scope)

            navigationManager.navigateBack
                .onEach {
                    println("cyka popBackStack")
                    navController.popBackStack()
                }
                .launchIn(scope)

            navigationManager.drawerState
                .onEach { isOpened ->
                    println("cyka open: $isOpened")
                    scope.launch {
                        if (isOpened) scaffoldState.drawerState.open()
                        else scaffoldState.drawerState.close()
                    }
                }
                .launchIn(scope)
        }
    }

}

