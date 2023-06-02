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

import com.wire.kalium.logic.data.asset.AttachmentType
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.kalium.logic.feature.conversation.InteractionAvailability
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType
import com.wire.kalium.logic.feature.selfDeletingMessages.SelfDeletionTimer
import kotlin.time.Duration.Companion.ZERO

data class MessageComposerViewState(
    val isFileSharingEnabled: Boolean = true,
    val securityClassificationType: SecurityClassificationType = SecurityClassificationType.NONE,
    val interactionAvailability: InteractionAvailability = InteractionAvailability.ENABLED,
    val mentionsToSelect: List<Contact> = listOf(),
    val assetTooLargeDialogState: AssetTooLargeDialogState = AssetTooLargeDialogState.Hidden,
    val selfDeletionTimer: SelfDeletionTimer = SelfDeletionTimer.Enabled(ZERO)
)

sealed class AssetTooLargeDialogState {
    object Hidden : AssetTooLargeDialogState()
    data class Visible(val assetType: AttachmentType, val maxLimitInMB: Int, val savedToDevice: Boolean) : AssetTooLargeDialogState()
}
