package com.wire.android.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import com.wire.android.ui.main.MainScreen
import com.wire.android.ui.welcome.WelcomeScreen

// Here could be an entry point for the app navigation
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Composable
fun WireApp() {
    MainScreen()
}

