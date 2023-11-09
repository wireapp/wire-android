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

package com.wire.android.ui.home.conversations

import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.kalium.logic.data.asset.AttachmentType
import com.wire.kalium.logic.data.message.SelfDeletionTimer
import com.wire.kalium.logic.feature.conversation.InteractionAvailability
import kotlin.time.Duration.Companion.ZERO

data class MessageComposerViewState(
    val isFileSharingEnabled: Boolean = true,
    val interactionAvailability: InteractionAvailability = InteractionAvailability.ENABLED,
    val mentionSearchResult: List<Contact> = listOf(),
    val selfDeletionTimer: SelfDeletionTimer = SelfDeletionTimer.Enabled(ZERO)
)

sealed class AssetTooLargeDialogState {
    object Hidden : AssetTooLargeDialogState()
    data class Visible(val assetType: AttachmentType, val maxLimitInMB: Int, val savedToDevice: Boolean) : AssetTooLargeDialogState()
}

sealed class VisitLinkDialogState {
    object Hidden : VisitLinkDialogState()
    data class Visible(val link: String, val openLink: () -> Unit) : VisitLinkDialogState()
}

sealed class InvalidLinkDialogState {
    object Hidden : InvalidLinkDialogState()
    object Visible : InvalidLinkDialogState()
}
