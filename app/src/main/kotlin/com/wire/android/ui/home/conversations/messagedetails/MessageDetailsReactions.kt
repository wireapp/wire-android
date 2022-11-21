package com.wire.android.ui.home.conversations.messagedetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wire.android.ui.home.conversations.details.participants.folderWithElements

@Composable
fun MessageDetailsReactions(
    messageDetailsState: MessageDetailsState,
    lazyListState: LazyListState = rememberLazyListState()
) {
    Column {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.weight(weight = 1f, fill = true)
        ) {
            messageDetailsState.reactionsData.reactions.forEach {
                folderWithElements(
                    header = "${it.key} ${it.value.size}",
                    items = it.value,
                    onRowItemClicked = { },
                    showRightArrow = false
                )
            }
        }
    }
}
