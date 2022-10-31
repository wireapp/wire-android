package com.wire.android.ui.home.messagecomposer

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.wire.android.model.Clickable
import com.wire.android.ui.home.conversations.details.participants.GroupConversationParticipantItem
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant

@Composable
fun MentionSuggestionList(
    suggestions: List<UIParticipant>,
    lazyListState: LazyListState,
    onItemSelected: (UIParticipant) -> Unit
) {
    if (suggestions.isEmpty()) return

    LazyColumn(state = lazyListState) {
        items(count = suggestions.size) {
            GroupConversationParticipantItem(
                suggestions[it],
                clickable = remember { Clickable(enabled = true) { onItemSelected(suggestions[it]) } })
        }
    }
}
