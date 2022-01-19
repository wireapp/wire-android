package com.wire.android.ui

import androidx.compose.runtime.Composable
import com.wire.android.ui.conversation.call.Call
import com.wire.android.ui.conversation.mention.Mention

// Here could be an entry point for navigation part, for now I just redirect to ConversationScreen
@Composable
fun WireApp() {
    Mention()
}

