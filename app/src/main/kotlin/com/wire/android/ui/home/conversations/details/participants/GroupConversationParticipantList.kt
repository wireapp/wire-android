package com.wire.android.ui.home.conversations.details.participants

import android.content.Context
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.ui.home.conversationslist.folderWithElements
import com.wire.android.ui.theme.wireColorScheme

fun LazyListScope.participantsFoldersWithElements(
    context: Context,
    state: GroupConversationParticipantsState,
    onRowItemClicked: (UIParticipant) -> Unit
) {
    folderWithElements(
        header = context.getString(R.string.conversation_details_group_admins, state.data.allAdminsCount),
        items = state.data.admins,
        onRowItemClicked = onRowItemClicked
    )
    folderWithElements(
        header = context.getString(R.string.conversation_details_group_members, state.data.allParticipantsCount),
        items = state.data.participants,
        onRowItemClicked = onRowItemClicked
    )
}

fun LazyListScope.folderWithElements(
    header: String,
    items: List<UIParticipant>,
    onRowItemClicked: (UIParticipant) -> Unit
) = folderWithElements(
    header = header,
    items = items.associateBy { it.id.toString() },
    factory = {
        GroupConversationParticipantItem(
            uiParticipant = it,
            clickable = remember { Clickable(enabled = true) { onRowItemClicked(it) } }
        )
    },
    divider = {
        Divider(
            color = MaterialTheme.wireColorScheme.background,
            thickness = Dp.Hairline
        )
    }
)
