/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
package com.wire.android.ui.home.messagecomposer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.ui.home.conversations.MessageComposerViewState
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionStateHolder
import com.wire.android.ui.home.messagecomposer.state.MessageComposerStateHolder
import com.wire.android.ui.home.messagecomposer.state.MessageComposition
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionHolder
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionInputStateHolder
import com.wire.kalium.logic.feature.selfDeletingMessages.SelfDeletionTimer
import kotlin.time.Duration


//@Preview
//@Composable
//fun MessageComposerPreview() {
//    val messageComposerViewState = remember { mutableStateOf(MessageComposerViewState()) }
//    val messageComposition = remember { mutableStateOf(MessageComposition.DEFAULT) }
//    val selfDeletionTimer = remember { mutableStateOf(SelfDeletionTimer.Enabled(Duration.ZERO)) }
//
//    MessageComposer(
//        messageComposerStateHolder = MessageComposerStateHolder(
//            messageComposerViewState = messageComposerViewState,
//            messageCompositionInputStateHolder = MessageCompositionInputStateHolder(
//                messageComposition = messageComposition,
//                selfDeletionTimer = selfDeletionTimer
//            ),
//            messageCompositionHolder = MessageCompositionHolder(
//                context = LocalContext.current
//            ),
//            additionalOptionStateHolder = AdditionalOptionStateHolder(),
//            modalSheetState = modalSheetState
//        ),
//        messageListContent = { },
//        onChangeSelfDeletionClicked = { },
//        onSearchMentionQueryChanged = { },
//        onClearMentionSearchResult = { }
//    )
//}
