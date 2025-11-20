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

import android.os.Parcelable
import com.wire.kalium.logic.data.id.ConversationId
import kotlinx.parcelize.Parcelize

data class MediaGalleryNavArgs(
    val conversationId: ConversationId,
    val messageId: String,
    val isSelfAsset: Boolean,
    val isEphemeral: Boolean,
    val messageOptionsEnabled: Boolean,
    val cellAssetId: String?,
)

@Parcelize
data class MediaGalleryNavBackArgs(
    val messageId: String,
    val emoji: String? = null,
    val isSelfAsset: Boolean = false,
    val mediaGalleryActionType: MediaGalleryActionType,
    val cellAssetId: String? = null,
) : Parcelable

enum class MediaGalleryActionType {
    REPLY,
    REACT,
    DETAIL
}
