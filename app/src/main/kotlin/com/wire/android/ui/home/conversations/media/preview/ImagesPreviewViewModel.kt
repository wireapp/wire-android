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
package com.wire.android.ui.home.conversations.media.preview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import com.wire.android.navigation.SavedStateViewModel
import com.wire.android.ui.navArgs
import javax.inject.Inject

class ImagesPreviewViewModel @Inject constructor(
    override val savedStateHandle: SavedStateHandle,
) : SavedStateViewModel(savedStateHandle) {

    private val navArgs: ImagesPreviewNavArgs = savedStateHandle.navArgs()
    var viewState by mutableStateOf(
        ImagesPreviewState(
            conversationId = navArgs.conversationId,
            conversationName = navArgs.conversationName,
            assetUri = navArgs.assetUri
        )
    )
        private set

}
