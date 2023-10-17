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
 *
 *
 */

package com.wire.android.ui.home.conversationslist.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.wire.android.R
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.home.conversationslist.common.ConversationList
import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import kotlinx.collections.immutable.ImmutableMap

@Composable
fun SearchConversationScreen(
    searchQuery: String,
    conversationSearchResult: ImmutableMap<ConversationFolder, List<ConversationItem>>,
    onOpenNewConversation: () -> Unit,
    onOpenConversation: (ConversationId) -> Unit,
    onEditConversation: (ConversationItem) -> Unit,
    onOpenUserProfile: (UserId) -> Unit,
    onJoinCall: (ConversationId) -> Unit,
    onPermanentPermissionDecline: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        if (conversationSearchResult.values.isEmpty()) {
            EmptySearchResult(onOpenNewConversation)
        } else {
            ConversationList(
                conversationListItems = conversationSearchResult,
                searchQuery = searchQuery,
                onOpenConversation = onOpenConversation,
                onEditConversation = onEditConversation,
                onOpenUserProfile = onOpenUserProfile,
                onJoinCall = onJoinCall,
                onPermanentPermissionDecline = onPermanentPermissionDecline
            )
        }
    }
}

@Composable
private fun EmptySearchResult(onNewConversationCLick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        VerticalSpace.x8()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier
                .padding(horizontal = dimensions().spacing48x)
                .wrapContentHeight()
        ) {
            Text(
                text = stringResource(R.string.label_no_conversation_found),
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.secondaryText,
                textAlign = TextAlign.Center
            )
            VerticalSpace.x16()
            Text(
                text = stringResource(R.string.label_connect_with_new_users),
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.secondaryText,
                textAlign = TextAlign.Center
            )
        }
        VerticalSpace.x16()
        WirePrimaryButton(
            text = stringResource(R.string.label_new_conversation),
            fillMaxWidth = false,
            minSize = dimensions().buttonSmallMinSize,
            minClickableSize = dimensions().buttonMinClickableSize,
            onClick = onNewConversationCLick
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewEmptySearchResult() {
    WireTheme {
        EmptySearchResult(onNewConversationCLick = {})
    }
}
