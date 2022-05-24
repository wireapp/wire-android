package com.wire.android.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.wire.android.navigation.NavigationGraph
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.navigateToItem
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.setScreenSettingsOnStart
import com.wire.android.util.ui.updateScreenSettings
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@AndroidEntryPoint
class WireActivity : AppCompatActivity() {

    @Inject
    lateinit var navigationManager: NavigationManager

    val viewModel: WireActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        println("cyka111 create ${intent.data}")
        viewModel.handleDeepLink(intent)
        setComposableContent()
    }

    override fun onNewIntent(intent: Intent?) {
        if (viewModel.handleDeepLinkOnNewIntent(intent)) {
            recreate()
        }
//        intent?.let {
//            println("cyka111 recreate")
//            recreate()
//            handleDeepLink(intent)
//        }
        super.onNewIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.activityOnResume()
    }

    override fun onPause() {
        super.onPause()
        viewModel.activityOnPause()
    }

    private fun setComposableContent() {
        setContent {
            WireTheme {
                val scope = rememberCoroutineScope()
                val navController = rememberAnimatedNavController()
                val startDestination = viewModel.startNavigationRoute()
                Scaffold {
                    NavigationGraph(navController = navController, startDestination, viewModel.navigationArguments())
                }
                setUpNavigation(navController, scope)
                setScreenSettingsOnStart(startDestination)
            }
        }
    }

    @Composable
    private fun setUpNavigation(
        navController: NavHostController,
        scope: CoroutineScope
    ) {
        println("cyka start rout: ${viewModel.startNavigationRoute()}")
        val keyboardController = LocalSoftwareKeyboardController.current
        // with the static key here we're sure that this effect wouldn't be canceled or restarted
        LaunchedEffect("key") {

            navigationManager.navigateState
                .onEach { command ->
                    if (command == null) return@onEach
                    keyboardController?.hide()
                    navigateToItem(navController, command)
                    updateScreenSettings(navController)
                }
                .launchIn(scope)

            navigationManager.navigateBack
                .onEach {
                    keyboardController?.hide()
                    if (!navController.popBackStack()) finish()
                    updateScreenSettings(navController)
                }
                .launchIn(scope)
        }
    }
}
