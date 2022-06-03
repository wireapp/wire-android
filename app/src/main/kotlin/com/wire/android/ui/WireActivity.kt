package com.wire.android.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.wire.android.navigation.NavigationGraph
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.navigateToItem
import com.wire.android.ui.server.ClientUpdateRequiredDialog
import com.wire.android.ui.server.ServerVersionNotSupportedDialog
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.CustomTabsHelper
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
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        viewModel.handleDeepLink(intent)
        setComposableContent {
            splashScreen.setKeepOnScreenCondition { it }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            viewModel.handleDeepLink(intent)
        }
    }

    private fun setComposableContent(keepSplashScreen: (Boolean) -> Unit) {
        setContent {
            WireTheme {
                val scope = rememberCoroutineScope()
                val navController = rememberAnimatedNavController()
                val state: WireActivityState = viewModel.state
                setUpNavigation(navController, scope)
                Scaffold { internalPadding ->
                    Box(modifier = Modifier.padding(internalPadding)) {
                        when (state) {
                            WireActivityState.ServerVersionNotSupported -> {
                                keepSplashScreen(false)
                                ServerVersionNotSupportedDialog(
                                    onClose = { this@WireActivity.finish() }
                                )
                            }
                            is WireActivityState.ClientUpdateRequired -> {
                                keepSplashScreen(false)
                                ClientUpdateRequiredDialog(
                                    onUpdate = {
                                        CustomTabsHelper.launchUrl(this@WireActivity, state.clientUpdateUrl)
                                        this@WireActivity.finish()
                                    },
                                    onClose = { this@WireActivity.finish() }
                                )
                            }
                            is WireActivityState.NavigationGraph -> {
                                keepSplashScreen(false)
                                NavigationGraph(
                                    navController = navController,
                                    startDestination = state.startNavigationRoute,
                                    appInitialArgs = state.navigationArguments
                                )
                            }
                            WireActivityState.Loading -> {
                                keepSplashScreen(true)
                            }
                        }
                    }
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
