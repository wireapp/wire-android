package com.wire.android.ui.home.conversationslist

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.ArrowLeftIcon
import com.wire.android.ui.common.bottomsheet.MenuModalSheetLayout
import com.wire.android.ui.common.bottomsheet.RichMenuBottomSheetItem

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MutingOptionsSheetContent(
    mutingConversationState: MutingConversationState = rememberMutingConversationState(),
    onItemClick: () -> Unit,
    onBackClick: () -> Unit
) {
    MenuModalSheetLayout(
        sheetState = mutingConversationState.sheetState,
        headerTitle = stringResource(R.string.label_notifications),
        menuItems = listOf(
            {
                RichMenuBottomSheetItem(
                    title = "Everything",
                    subLine = "Receive notifications for this conversation about everything (including audio and video calls)",
                    action = { },
                    onItemClick = onItemClick
                )
            },
            {
                RichMenuBottomSheetItem(
                    title = "Mentions and replies",
                    subLine = "Only receive notifications for this conversation when someone mentions you or replies to you",
                    action = {},
                    onItemClick = onItemClick
                )
            },
            {
                RichMenuBottomSheetItem(
                    title = "Nothing",
                    subLine = "Receive no notifications for this conversation at all",
                    action = {},
                    onItemClick = onItemClick
                )
            }
        ),
        headerIcon = { ArrowLeftIcon() },
        headerAction = onBackClick
    ) {}
}
