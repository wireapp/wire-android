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
package com.wire.android.util

import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.user.SupportedProtocol
import com.wire.kalium.logic.feature.featureConfig.AppsAllowedProtocol
import com.wire.kalium.logic.feature.featureConfig.AppsAllowedResult

object AppsUtil {

    fun isAppsAllowed(
        appsAllowedResult: AppsAllowedResult?,
        conversationProtocol: Conversation.ProtocolInfo?
    ) = when (appsAllowedResult) {
        is AppsAllowedResult.Enabled -> when (appsAllowedResult.protocol) {
            AppsAllowedProtocol.MLS -> true
            AppsAllowedProtocol.PROTEUS -> false
            is AppsAllowedProtocol.MIXED -> when (conversationProtocol) {
                is Conversation.ProtocolInfo.MLS ->
                    (appsAllowedResult.protocol as AppsAllowedProtocol.MIXED).defaultProtocol == SupportedProtocol.MLS
                is Conversation.ProtocolInfo.Proteus -> false
                null, is Conversation.ProtocolInfo.Mixed ->
                    (appsAllowedResult.protocol as AppsAllowedProtocol.MIXED).defaultProtocol == SupportedProtocol.MLS
            }
        }
        null, is AppsAllowedResult.Disabled -> false
    }
}
