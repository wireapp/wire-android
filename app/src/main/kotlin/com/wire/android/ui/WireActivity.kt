package com.wire.android.ui

import android.annotation.SuppressLint
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
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.VoyagerNavigationGraph
import com.wire.android.navigation.navigateToItem
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.ui.updateScreenSettings
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class,
    ExperimentalCoroutinesApi::class
)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@AndroidEntryPoint
class WireActivity : AppCompatActivity() {

    @Inject
    lateinit var navigationManager: NavigationManager

    @Inject
    lateinit var currentScreenManager: CurrentScreenManager

    val viewModel: WireActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(currentScreenManager)
        viewModel.handleDeepLink(intent)
        setComposableContent()
    }

    override fun onNewIntent(intent: Intent?) {
        viewModel.handleDeepLinkOnNewIntent(intent)
        super.onNewIntent(intent)
    }

    private fun setComposableContent() {
        setContent {
            WireTheme {
                val scope = rememberCoroutineScope()
                Scaffold {
                    VoyagerNavigationGraph(
                        startScreens = viewModel.startVoyagerNavigationScreen()
                    ) {
                        CurrentScreen()
                        setUpVoyagerNavigation(navigator = it, scope = scope)
                    }
                }
            }
        }
    }

    @Composable
    private fun setUpVoyagerNavigation(
        navigator: Navigator,
        scope: CoroutineScope
    ) {
        val keyboardController = LocalSoftwareKeyboardController.current
        // with the static key here we're sure that this effect wouldn't be canceled or restarted
        LaunchedEffect("key") {
            navigationManager.navigateState
                .onEach { command ->
                    if (command == null) return@onEach
                    keyboardController?.hide()
                    navigator.navigateToItem(command)
                }
                .launchIn(scope)

            navigationManager.navigateBack
                .onEach {
                    if (!navigator.pop()) finish()
                }
                .launchIn(scope)
        }

        LaunchedEffect(navigator.lastItem) {
            currentScreenManager.onDestinationChanged(navigator.lastItem)
            keyboardController?.hide()
            updateScreenSettings(navigator)
        }
    }
}
