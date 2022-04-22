package com.wire.android.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.wire.android.R
import com.wire.android.navigation.NavigationGraph
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.navigateToItem
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.deeplink.DeepLinkResult
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
        handleDeepLink(intent)
        setComposableContent()
    }

    private fun handleDeepLink(intent: Intent) {
        viewModel.handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        intent?.let {
            recreate()
            viewModel.handleDeepLink(intent)
        }
        super.onNewIntent(intent)
    }

    private fun setComposableContent() {
        setContent {
            WireTheme {
                val scope = rememberCoroutineScope()
                val navController = rememberAnimatedNavController()
                setUpNavigation(navController, scope)
                Scaffold {
                    NavigationGraph(navController = navController, viewModel.startNavigationRoute(), viewModel.navigationArguments())
                }
            }
        }
    }



    @Composable
    private fun setUpNavigation(
        navController: NavHostController,
        scope: CoroutineScope
    ) {
        val keyboardController = LocalSoftwareKeyboardController.current
        // with the static key here we're sure that this effect wouldn't be canceled or restarted
        LaunchedEffect("key") {

            navigationManager.navigateState.onEach { command ->
                if (command == null) return@onEach
                keyboardController?.hide()
                navigateToItem(navController, command)
            }.launchIn(scope)

            navigationManager.navigateBack
                .onEach {
                    keyboardController?.hide()
                    navController.popBackStack()
                }
                .launchIn(scope)
        }
    }
}
