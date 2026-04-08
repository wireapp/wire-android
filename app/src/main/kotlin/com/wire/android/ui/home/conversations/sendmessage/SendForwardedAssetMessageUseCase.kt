/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

package com.wire.android.ui.home.conversations.sendmessage

import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.AssetContent
import com.wire.kalium.logic.feature.message.MessageScope
import com.wire.kalium.logic.feature.message.SendExistingAssetMessageResult
import javax.inject.Inject

class SendForwardedAssetMessageUseCase @Inject constructor(
    private val messageScope: MessageScope,
) {
    suspend operator fun invoke(
        conversationId: ConversationId,
        assetContent: AssetContent,
    ): Result = when (val result = messageScope.sendExistingAssetMessage(conversationId, assetContent)) {
        SendExistingAssetMessageResult.Success -> Result.Success
        SendExistingAssetMessageResult.Failure.DisabledByTeam -> Result.Failure.DisabledByTeam
        SendExistingAssetMessageResult.Failure.RestrictedFileType -> Result.Failure.RestrictedFileType
        is SendExistingAssetMessageResult.Failure.Generic -> Result.Failure.Generic(result.coreFailure)
    }

    sealed interface Result {
        data object Success : Result

        sealed interface Failure : Result {
            data object DisabledByTeam : Failure
            data object RestrictedFileType : Failure
            data class Generic(val coreFailure: CoreFailure) : Failure
        }
    }
}
