package com.wire.android.ui.authentication.create.personalaccount

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.wire.android.navigation.smoothSlideInFromRight
import com.wire.android.navigation.smoothSlideOutFromLeft
import com.wire.kalium.logic.configuration.ServerConfig
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@OptIn(ExperimentalMaterialApi::class, ExperimentalAnimationApi::class)
@Composable
fun CreatePersonalAccountScreen(
    serverConfig: ServerConfig
) {
    val viewModel: CreatePersonalAccountViewModel = hiltViewModel()
    val navController = rememberAnimatedNavController()
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxSize()) { // needed for the transition animations to work properly
        AnimatedNavHost(navController = navController, startDestination = CreatePersonalAccountNavigationItem.Overview.route) {
            CreatePersonalAccountNavigationItem.values().forEach { destination ->
                composable(
                    route = destination.route,
                    enterTransition = {smoothSlideInFromRight() },
                    exitTransition = { smoothSlideOutFromLeft() },
                    content = { destination.content(ContentParams(viewModel, serverConfig)) }
                )
            }
        }
        LaunchedEffect(viewModel) {
            viewModel.moveToStep.onEach { item -> navigateToItemInCreatePersonalAccount(navController, item) }.launchIn(scope)
            viewModel.moveBack.onEach { if (!navController.popBackStack()) viewModel.closeForm() }.launchIn(scope)
        }
    }
}
