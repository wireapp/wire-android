package com.wire.android.feature.conversation.content.navigation

import android.content.Context
import com.wire.android.feature.conversation.content.ui.ConversationActivity

class ConversationNavigator {
    fun openConversationScreen(context: Context, conversationId: String, conversationTitle: String) =
        context.startActivity(ConversationActivity.newIntent(context, conversationId, conversationTitle))
}
