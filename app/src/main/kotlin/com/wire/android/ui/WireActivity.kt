package com.wire.android.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.wire.android.navigation.NavigationGraph
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.navigateToItem
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.userprofile.image.ProfileImageScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@AndroidEntryPoint
class WireActivity : AppCompatActivity() {

    @Inject
    lateinit var navigationManager: NavigationManager

    override fun onCreate(savedInstanceState: Bundle?) {
//        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            WireTheme {

                ProfileImageScreen("")
            }
        }
    }

    @Composable
    private fun setUpNavigation(
        navController: NavHostController,
        scope: CoroutineScope
    ) {
        // with the static key here we're sure that this effect wouldn't be canceled or restarted
        LaunchedEffect("key") {

            navigationManager.navigateState
                .onEach { command ->
                    if (command == null) return@onEach
                    navigateToItem(navController, command)
                }
                .launchIn(scope)

            navigationManager.navigateBack
                .onEach { navController.popBackStack() }
                .launchIn(scope)
        }
    }
}
