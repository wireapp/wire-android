package com.wire.android.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import com.wire.android.ui.conversation.all.Conversation
import com.wire.android.ui.conversation.call.Call
import com.wire.android.ui.conversation.mention.Mention
import com.wire.android.ui.main.MainScreen

// Here could be an entry point for the app navigation
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Composable
fun WireApp() {
    Conversation()
}

