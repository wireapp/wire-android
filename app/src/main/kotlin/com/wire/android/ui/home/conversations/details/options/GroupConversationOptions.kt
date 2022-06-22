package com.wire.android.ui.home.conversations.details

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsState

@Composable
fun GroupConversationOptions(
    groupOptionsState: GroupConversationOptionsState,
    lazyListState: LazyListState = rememberLazyListState()
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            GroupConversationOptionsItem(
                label = stringResource(id = R.string.conversation_details_options_group_name),
                title = groupOptionsState.groupName,
                onClick = { /* TODO */ }
            )
            Divider()
        }
    }
}
