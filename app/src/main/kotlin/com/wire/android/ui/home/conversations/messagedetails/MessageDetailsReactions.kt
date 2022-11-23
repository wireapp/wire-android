package com.wire.android.ui.home.conversations.messagedetails

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.details.participants.folderWithElements

@Composable
fun MessageDetailsReactions(
    messageDetailsState: MessageDetailsState,
    lazyListState: LazyListState = rememberLazyListState(),
    onReactionsLearnMore: () -> Unit
) {
    Column {
        if (messageDetailsState.reactionsData.reactions.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(weight = 1f, fill = true)
                    .align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = stringResource(id = R.string.message_details_reactions_empty_text),
                    textAlign = TextAlign.Center
                )
                val learnMoreText = stringResource(id = R.string.message_details_reactions_empty_learn_more)
                val learnMore = buildAnnotatedString {
                    append(learnMoreText)
                    addStyle(
                        style = SpanStyle(
                            color = Color.Blue,
                            textDecoration = TextDecoration.Underline
                        ),
                        start = 0,
                        end = learnMoreText.length
                    )
                }
                ClickableText(
                    text = learnMore,
                    modifier = Modifier.padding(top = dimensions().spacing48x),
                    onClick = { onReactionsLearnMore() }
                )
            }
        } else {
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
}

@Preview(showBackground = true)
@Composable
fun MessageDetailsReactionsPreview() {
    MessageDetailsReactions(
        messageDetailsState = MessageDetailsState(),
        onReactionsLearnMore = {}
    )
}
