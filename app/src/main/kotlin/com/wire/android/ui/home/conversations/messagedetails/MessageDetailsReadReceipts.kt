/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.ui.home.conversations.messagedetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.home.conversations.details.participants.sectionWithElements
import com.wire.android.ui.home.conversations.messagedetails.model.MessageDetailsReadReceiptsData
import com.wire.android.ui.theme.WireTheme

@Composable
fun MessageDetailsReadReceipts(
    readReceiptsData: MessageDetailsReadReceiptsData,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    onReadReceiptsLearnMore: () -> Unit
) {
    Column(modifier = modifier) {
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
                sectionWithElements(
                    header = "",
                    items = readReceiptsData.readReceipts,
                    onRowItemClicked = { },
                    showRightArrow = false
                )
            }
        }
    }
}

@MultipleThemePreviews
@Composable
fun PreviewMessageDetailsReadReceipts() {
    WireTheme {
        MessageDetailsReadReceipts(
            readReceiptsData = MessageDetailsReadReceiptsData(),
            onReadReceiptsLearnMore = {}
        )
    }
}
