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

package com.wire.android.ui.home.gallery

import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogActiveState
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogsState

data class MediaGalleryViewState(
    val screenTitle: String? = null,
    val isEphemeral: Boolean = false,
    val deleteMessageDialogsState: DeleteMessageDialogsState = DeleteMessageDialogsState.States(
        forYourself = DeleteMessageDialogActiveState.Hidden,
        forEveryone = DeleteMessageDialogActiveState.Hidden
    ),
    val messageBottomSheetOptionsEnabled: Boolean
)
