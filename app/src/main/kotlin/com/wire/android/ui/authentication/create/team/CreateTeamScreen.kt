package com.wire.android.ui.authentication.create.team

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CreateTeamScreen(viewModel: CreateTeamViewModel) {
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxSize()) { // needed for the transition animations to work properly
        Navigator(screen = CreateTeamNavigationItem.Overview(viewModel)) { navigator ->
            val keyboardController = LocalSoftwareKeyboardController.current
            val focusManager = LocalFocusManager.current
            LaunchedEffect(viewModel) {
                viewModel.moveToStep.onEach { item ->
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                    navigator.push(item)
                }.launchIn(scope)
                viewModel.moveBack.onEach {
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                    if(!navigator.pop()) viewModel.closeForm()
                }.launchIn(scope)
            }
            CurrentScreen()
        }
    }
}
