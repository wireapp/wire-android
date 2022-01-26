package com.wire.android.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import com.wire.android.ui.main.MainScreen
import com.wire.android.ui.main.message.PreviewMessage

// Here could be an entry point for the app navigation
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Composable
fun WireApp() {
    PreviewMessage()
}

