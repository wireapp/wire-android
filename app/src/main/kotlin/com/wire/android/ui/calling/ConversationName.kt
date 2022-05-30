package com.wire.android.ui.calling

import com.wire.android.R

sealed class ConversationName {
    data class Known(val name: String) : ConversationName()
    data class Unknown(val resourceId: Int) : ConversationName()
}

fun getConversationName(name: String?): ConversationName {
    return name?.let {
        ConversationName.Known(it)
    } ?: run {
        ConversationName.Unknown(R.string.calling_label_default_caller_name)
    }
}
