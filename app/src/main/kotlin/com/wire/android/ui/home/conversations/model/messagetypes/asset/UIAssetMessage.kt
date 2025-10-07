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
package com.wire.android.ui.home.conversations.model.messagetypes.asset

import androidx.compose.runtime.Stable
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.id.QualifiedID
import kotlin.time.Instant
import okio.Path

@Stable
data class UIAssetMessage(
    val assetId: String,
    val time: Instant,
    val username: UIText,
    val messageId: String,
    val conversationId: QualifiedID,
    val assetPath: Path?,
    val isSelfAsset: Boolean
)
