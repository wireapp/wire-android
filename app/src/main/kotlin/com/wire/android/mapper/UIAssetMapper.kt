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
package com.wire.android.mapper

import com.wire.android.R
import com.wire.android.ui.home.conversations.model.MessageTime
import com.wire.android.ui.home.conversations.model.messagetypes.asset.UIAsset
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.asset.AssetMessage
import javax.inject.Inject

class UIAssetMapper @Inject constructor() {

    fun toUIAsset(assetMessage: AssetMessage): UIAsset {
        return UIAsset(
            assetId = assetMessage.assetId,
            time = assetMessage.time,
            username = assetMessage.username?.let { UIText.DynamicString(it) }
                ?: UIText.StringResource(R.string.username_unavailable_label),
            conversationId = assetMessage.conversationId,
            messageId = assetMessage.messageId,
            assetPath = assetMessage.assetPath,
            downloadStatus = assetMessage.downloadStatus,
            isSelfAsset = assetMessage.isSelfAsset
        )
    }
}
