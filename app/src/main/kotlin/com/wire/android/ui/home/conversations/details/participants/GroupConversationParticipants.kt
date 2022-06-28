package com.wire.android.ui.home.conversations.details.participants

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.wire.android.R
import com.wire.android.ui.home.conversations.model.UIParticipant
import com.wire.android.ui.home.conversationslist.folderWithElements
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.stringWithStyledArgs
import com.wire.kalium.logic.data.user.UserId

@Composable
fun GroupConversationParticipants(
    groupParticipantsState: GroupConversationParticipantsState,
    lazyListState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    Column {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.weight(weight = 1f, fill = true)
        ) {
            item {
                Text(
                    text = context.resources.stringWithStyledArgs(
                        R.string.conversation_details_participants_info,
                        MaterialTheme.wireTypography.body01,
                        MaterialTheme.wireTypography.body02,
                        MaterialTheme.wireColorScheme.onBackground,
                        MaterialTheme.wireColorScheme.onBackground,
                        (groupParticipantsState.allParticipantsCount + groupParticipantsState.allAdminsCount).toString()
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.wireColorScheme.surface)
                        .padding(MaterialTheme.wireDimensions.spacing16x)
                )
            }
            folderWithElements(
                header = context.getString(R.string.conversation_details_group_admins, groupParticipantsState.allAdminsCount),
                items = groupParticipantsState.admins
            )
            folderWithElements(
                header = context.getString(R.string.conversation_details_group_members, groupParticipantsState.allParticipantsCount),
                items = groupParticipantsState.participants
            )
        }
    }
}

private fun LazyListScope.folderWithElements(header: String, items: List<UIParticipant>) = folderWithElements(
    header = header,
    items = items.associateBy { it.id.toString() },
    factory = { GroupConversationParticipantItem(it) },
    divider = { Divider(color = MaterialTheme.wireColorScheme.background, thickness = Dp.Hairline) }
)

@Preview
@Composable
fun GroupConversationParticipantsPreview() {
    GroupConversationParticipants(
        GroupConversationParticipantsState(
            participants = listOf(UIParticipant(UserId("1", ""), "name", "handle", false))
        )
    )
}
