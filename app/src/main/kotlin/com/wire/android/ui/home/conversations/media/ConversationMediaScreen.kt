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

package com.wire.android.ui.home.conversations.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.R
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.destinations.MediaGalleryScreenDestination
import com.wire.kalium.logic.data.id.ConversationId

@RootNavGraph
@Destination(
    navArgsDelegate = ConversationMediaNavArgs::class,
    style = PopUpNavigationAnimation::class
)
@Composable
fun ConversationMediaScreen(navigator: Navigator) {
    val viewModel: ConversationAssetMessagesViewModel = hiltViewModel()
    val state: ConversationAssetMessagesViewState = viewModel.viewState

    Content(
        state = state,
        onNavigationPressed = { navigator.navigateBack() },
        onImageFullScreenMode = { conversationId, messageId, isSelfAsset ->
            navigator.navigate(
                NavigationCommand(
                    MediaGalleryScreenDestination(
                        conversationId = conversationId,
                        messageId = messageId,
                        isSelfAsset = isSelfAsset,
                        isEphemeral = false
                    )
                )
            )
        },
        continueAssetLoading = { shouldContinue ->
            viewModel.continueLoading(shouldContinue)
        }
    )
}

@Composable
private fun Content(
    state: ConversationAssetMessagesViewState,
    onNavigationPressed: () -> Unit = {},
    onImageFullScreenMode: (conversationId: ConversationId, messageId: String, isSelfAsset: Boolean) -> Unit,
    continueAssetLoading: (shouldContinue: Boolean) -> Unit
) {
    WireScaffold(
        modifier = Modifier
            .background(color = colorsScheme().backgroundVariant),
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = 0.dp,
                title = stringResource(id = R.string.label_conversation_media),
                navigationIconType = NavigationIconType.Back,
                onNavigationPressed = onNavigationPressed
            )
        },
    ) { padding ->
        // TODO implement tab here for https://wearezeta.atlassian.net/browse/WPB-5378
        AssetList(
            uiAssetList = state.messages,
            modifier = Modifier.padding(padding),
            onImageFullScreenMode = onImageFullScreenMode,
            continueAssetLoading = continueAssetLoading
        )
    }
}
