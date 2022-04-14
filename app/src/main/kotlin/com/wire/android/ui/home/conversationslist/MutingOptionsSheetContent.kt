package com.wire.android.ui.home.conversationslist

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.ArrowLeftIcon
import com.wire.android.ui.common.bottomsheet.MenuModalSheetLayout
import com.wire.android.ui.common.bottomsheet.RichMenuBottomSheetItem
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MutingOptionsSheetContent(
    mutingConversationState: MutingConversationState = rememberMutingConversationState(),
    onItemClick: (ConversationId?, MutedConversationStatus) -> Unit,
    onBackClick: () -> Unit
) {
    MenuModalSheetLayout(
        sheetState = mutingConversationState.sheetState,
        headerTitle = stringResource(R.string.label_notifications),
        menuItems = listOf(
            {
                RichMenuBottomSheetItem(
                    title = stringResource(id = R.string.muting_option_all_allowed_title),
                    subLine = stringResource(id = R.string.muting_option_all_allowed_text),
                    action = { },
                    onItemClick = { onItemClick(mutingConversationState.conversationId, MutedConversationStatus.AllAllowed) }
                )
            },
            {
                RichMenuBottomSheetItem(
                    title = stringResource(id = R.string.muting_option_only_mentions_title),
                    subLine = stringResource(id = R.string.muting_option_only_mentions_text),
                    action = {},
                    onItemClick = { onItemClick(mutingConversationState.conversationId, MutedConversationStatus.OnlyMentionsAllowed) }
                )
            },
            {
                RichMenuBottomSheetItem(
                    title = stringResource(id = R.string.muting_option_all_muted_title),
                    subLine = stringResource(id = R.string.muting_option_all_muted_text),
                    action = {},
                    onItemClick = { onItemClick(mutingConversationState.conversationId, MutedConversationStatus.AllMuted) }
                )
            }
        ),
        headerIcon = { ArrowLeftIcon() },
        headerAction = onBackClick
    ) {}
}
