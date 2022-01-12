package com.wire.android.feature.conversation.content.navigation

import android.content.Context
import com.wire.android.feature.conversation.ConversationID
import com.wire.android.feature.conversation.content.ui.ConversationActivity

class ConversationNavigator {
    fun openConversationScreen(context: Context, conversationIdValue: String, conversationDomain: String, conversationTitle: String) =
        context.startActivity(ConversationActivity.newIntent(context, conversationIdValue, conversationDomain, conversationTitle))
}
