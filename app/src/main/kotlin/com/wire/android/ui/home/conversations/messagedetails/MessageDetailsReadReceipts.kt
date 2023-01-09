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
import com.wire.android.ui.home.conversations.messagedetails.model.MessageDetailsReadReceiptsData

@Composable
fun MessageDetailsReadReceipts(
    readReceiptsData: MessageDetailsReadReceiptsData,
    lazyListState: LazyListState = rememberLazyListState(),
    onReadReceiptsLearnMore: () -> Unit
) {
    Column {
        if (readReceiptsData.readReceipts.isEmpty()) {
            MessageDetailsEmptyScreenText(
                onClick = onReadReceiptsLearnMore,
                modifier = Modifier
                    .weight(weight = 1f, fill = true)
                    .align(Alignment.CenterHorizontally),
                text = stringResource(id = R.string.message_details_read_receipts_empty_text),
                learnMoreText = stringResource(id = R.string.message_details_read_receipts_empty_learn_more)
            )
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.weight(weight = 1f, fill = true)
            ) {
                folderWithElements(
                    header = "",
                    items = readReceiptsData.readReceipts,
                    onRowItemClicked = { },
                    showRightArrow = false
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MessageDetailsReadReceiptsPreview() {
    MessageDetailsReadReceipts(
        readReceiptsData = MessageDetailsReadReceiptsData(),
        onReadReceiptsLearnMore = {}
    )
}
