package com.wire.android.ui.home.conversationslist

import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.ArrowLeftIcon
import com.wire.android.ui.common.bottomsheet.MenuModalSheetLayout
import com.wire.android.ui.common.bottomsheet.RichMenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.RichMenuItemState
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MutingOptionsSheetContent(
    mutingConversationState: MutingConversationState = rememberMutingConversationState(),
    onItemClick: (ConversationId?, MutedConversationStatus) -> Unit,
    onBackClick: () -> Unit,
    currentStatus: MutedConversationStatus = MutedConversationStatus.AllAllowed
) {
    MenuModalSheetLayout(
        sheetState = mutingConversationState.sheetState,
        headerTitle = stringResource(R.string.label_notifications),
        menuItems = listOf(
            {
                RichMenuBottomSheetItem(
                    title = stringResource(id = R.string.muting_option_all_allowed_title),
                    subLine = stringResource(id = R.string.muting_option_all_allowed_text),
                    action = { CheckIcon() },
                    onItemClick = { onItemClick(mutingConversationState.conversationId, MutedConversationStatus.AllAllowed) },
                    state = if (currentStatus == MutedConversationStatus.AllAllowed) RichMenuItemState.SELECTED
                    else RichMenuItemState.DEFAULT
                )
            },
            {
                RichMenuBottomSheetItem(
                    title = stringResource(id = R.string.muting_option_only_mentions_title),
                    subLine = stringResource(id = R.string.muting_option_only_mentions_text),
                    action = { CheckIcon() },
                    onItemClick = { onItemClick(mutingConversationState.conversationId, MutedConversationStatus.OnlyMentionsAllowed) },
                    state = if (currentStatus == MutedConversationStatus.OnlyMentionsAllowed) RichMenuItemState.SELECTED
                    else RichMenuItemState.DEFAULT
                )
            },
            {
                RichMenuBottomSheetItem(
                    title = stringResource(id = R.string.muting_option_all_muted_title),
                    subLine = stringResource(id = R.string.muting_option_all_muted_text),
                    action = { CheckIcon() },
                    onItemClick = { onItemClick(mutingConversationState.conversationId, MutedConversationStatus.AllMuted) },
                    state = if (currentStatus == MutedConversationStatus.AllMuted) RichMenuItemState.SELECTED
                    else RichMenuItemState.DEFAULT
                )
            }
        ),
        headerIcon = { ArrowLeftIcon() },
        headerAction = onBackClick
    ) {}
}

@Composable
private fun CheckIcon() {
    Icon(
        painter = painterResource(id = R.drawable.ic_check_circle),
        contentDescription = stringResource(R.string.content_description_check),
        modifier = Modifier.size(MaterialTheme.wireDimensions.wireIconButtonSize),
        tint = MaterialTheme.wireColorScheme.positive
    )
}
