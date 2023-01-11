package com.wire.android.ui.home.conversations.messagedetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.home.conversations.details.participants.folderWithElements
import com.wire.android.ui.home.conversations.messagedetails.model.MessageDetailsReactionsData

@Composable
fun MessageDetailsReactions(
    reactionsData: MessageDetailsReactionsData,
    lazyListState: LazyListState = rememberLazyListState(),
    onReactionsLearnMore: () -> Unit
) {
    Column {
        if (reactionsData.reactions.isEmpty()) {
            MessageDetailsEmptyScreenText(
                onClick = onReactionsLearnMore,
                modifier = Modifier
                    .weight(weight = 1f, fill = true)
                    .align(Alignment.CenterHorizontally),
                text = stringResource(id = R.string.message_details_reactions_empty_text),
                learnMoreText = stringResource(id = R.string.message_details_reactions_empty_learn_more)
            )
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.weight(weight = 1f, fill = true)
            ) {
                reactionsData.reactions.forEach {
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
}

@Preview(showBackground = true)
@Composable
fun MessageDetailsReactionsPreview() {
    MessageDetailsReactions(
        reactionsData = MessageDetailsReactionsData(),
        onReactionsLearnMore = {}
    )
}
