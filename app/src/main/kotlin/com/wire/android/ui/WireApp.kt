package com.wire.android.ui

import androidx.compose.runtime.Composable
import com.wire.android.ui.welcome.WelcomeScreen

// Here could be an entry point for navigation part, for now I just redirect to ConversationScreen
@Composable
fun WireApp() {
    WelcomeScreen()
}

