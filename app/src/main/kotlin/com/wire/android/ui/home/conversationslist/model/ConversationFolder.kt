package com.wire.android.ui.home.conversationslist.model

import androidx.annotation.StringRes
import com.wire.android.R

sealed class ConversationFolder {
    sealed class Predefined(@StringRes val folderNameResId: Int): ConversationFolder() {
        object Conversations: Predefined(R.string.conversation_label_conversations)
        object Favorites: Predefined(R.string.conversation_label_favorites)
    }
    data class Custom(val folderName: String): ConversationFolder()
}
