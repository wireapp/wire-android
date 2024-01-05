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

package com.wire.android.ui.home.archive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.ramcosta.composedestinations.annotation.Destination
import com.wire.android.R
import com.wire.android.navigation.HomeNavGraph
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.HomeStateHolder
import com.wire.android.ui.home.conversationslist.ConversationItemType
import com.wire.android.ui.home.conversationslist.ConversationRouterHomeBridge
import com.wire.android.ui.home.conversationslist.model.ConversationsSource
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

/**
 * ArchiveScreen composable function.
 *
 * This screen leverages the ConversationRouterHomeBridge to render its UI and logic.
 * Reasons for using ConversationRouterHomeBridge:
 * 1. **Consistency**: Ensures a uniform UI/UX between the Archive and Conversation screens.
 * 2. **Code Efficiency**: Eliminates redundancy by reusing shared logic and components.
 * 3. **Flexibility**: Accommodates distinct data queries while retaining core UI logic.
 * 4. **Maintainability**: Centralizes updates, reducing potential bugs and inconsistencies.
 * 5. **Optimization**: Speeds up the development cycle by reusing established components.
 */
@HomeNavGraph
@Destination
@Composable
fun ArchiveScreen(homeStateHolder: HomeStateHolder) {
    with(homeStateHolder) {
        ConversationRouterHomeBridge(
            navigator = navigator,
            conversationItemType = ConversationItemType.ALL_CONVERSATIONS,
            onHomeBottomSheetContentChanged = ::changeBottomSheetContent,
            onOpenBottomSheet = ::openBottomSheet,
            onCloseBottomSheet = ::closeBottomSheet,
            onSnackBarStateChanged = ::setSnackBarState,
            searchBarState = searchBarState,
            isBottomSheetVisible = ::isBottomSheetVisible,
            conversationsSource = ConversationsSource.ARCHIVE
        )
    }
}

@Composable
fun ArchivedConversationsEmptyStateScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier.padding(
                bottom = dimensions().spacing24x,
            ),
            text = stringResource(R.string.archive_screen_empty_state_title),
            style = MaterialTheme.wireTypography.title01,
            color = MaterialTheme.wireColorScheme.onSurface,
        )
        Text(
            modifier = Modifier.padding(
                bottom = dimensions().spacing8x,
                start = dimensions().spacing40x,
                end = dimensions().spacing40x
            ),
            text = stringResource(R.string.archive_screen_empty_state_description),
            style = MaterialTheme.wireTypography.body01,
            textAlign = TextAlign.Center,
            color = MaterialTheme.wireColorScheme.secondaryText,
        )
    }
}

@Preview(showBackground = false)
@Composable
fun PreviewArchiveEmptyScreen() {
    ArchivedConversationsEmptyStateScreen()
}
