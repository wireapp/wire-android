/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 *
 */

package com.wire.android.ui.authentication.create.personalaccount

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.wire.android.navigation.rememberTrackingAnimatedNavController
import com.wire.android.navigation.smoothSlideInFromRight
import com.wire.android.navigation.smoothSlideOutFromLeft
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@OptIn(ExperimentalAnimationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun CreatePersonalAccountScreen() {
    val viewModel: CreatePersonalAccountViewModel = hiltViewModel()
    val navController = rememberTrackingAnimatedNavController() { CreatePersonalAccountNavigationItem.fromRoute(it)?.itemName }
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxSize()) { // needed for the transition animations to work properly
        AnimatedNavHost(navController = navController, startDestination = CreatePersonalAccountNavigationItem.Overview.route) {
            CreatePersonalAccountNavigationItem.values().forEach { destination ->
                composable(
                    route = destination.route,
                    enterTransition = { smoothSlideInFromRight() },
                    exitTransition = { smoothSlideOutFromLeft() },
                    content = { destination.content(ContentParams(viewModel)) }
                )
            }
        }
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current
        LaunchedEffect(viewModel) {
            viewModel.moveToStep.onEach { item ->
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
                navigateToItemInCreatePersonalAccount(navController, item)
            }.launchIn(scope)
            viewModel.moveBack.onEach {
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
                if (!navController.popBackStack()) viewModel.closeForm()
            }.launchIn(scope)
        }
    }
}
