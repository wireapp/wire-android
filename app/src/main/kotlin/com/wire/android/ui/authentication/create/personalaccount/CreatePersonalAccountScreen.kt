package com.wire.android.ui.authentication.create.personalaccount

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
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
    NavHost(navController = navController, startDestination = CreatePersonalAccountNavigationItem.Overview.route) {
        CreatePersonalAccountNavigationItem.values().forEach { destination ->
            composable(route = destination.route) { destination.content(ContentParams(viewModel, serverConfig)) }
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.moveToStep.onEach { item ->
            navigateToItemInCreatePersonalAccount(navController, item)
        }.launchIn(scope)
        viewModel.moveBack.onEach {
            if (!navController.popBackStack()) viewModel.closeForm()
        }.launchIn(scope)
    }
}
