package com.wire.android.ui.home.conversationslist.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.kalium.logic.data.conversation.MutedConversationStatus

@Composable
fun MutedConversationStatus.getMutedStatusTextResource(): String {
    return when (this) {
        MutedConversationStatus.OnlyMentionsAllowed -> stringResource(id = R.string.muting_option_only_mentions_title)
        MutedConversationStatus.AllMuted -> stringResource(id = R.string.muting_option_all_muted_title)
        else -> stringResource(id = R.string.muting_option_all_allowed_title)
    }
}
