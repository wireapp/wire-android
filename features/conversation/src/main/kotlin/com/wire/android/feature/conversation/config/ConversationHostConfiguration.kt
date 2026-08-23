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

package com.wire.android.feature.conversation.config

import androidx.compose.runtime.staticCompositionLocalOf

interface ConversationHostConfiguration {
    val runtime: ConversationRuntimeCapabilities
    val visibility: ConversationUiVisibility
}

data class ConversationRuntimeCapabilities(
    val bubbleUiEnabled: Boolean,
    val pendingMessagesEnabled: Boolean,
    val developerFeaturesEnabled: Boolean,
    val mlsReadReceiptsEnabled: Boolean,
    val privateBuild: Boolean,
    val passwordProtectedGuestLinksEnabled: Boolean,
)

data class ConversationUiVisibility(
    val audioMessages: Boolean,
    val shareLocation: Boolean,
    val drawing: Boolean,
    val emoji: Boolean,
    val gif: Boolean,
    val ping: Boolean,
    val topBarConversationSearch: Boolean,
    val messageSearch: Boolean,
)

val LocalConversationHostConfiguration = staticCompositionLocalOf<ConversationHostConfiguration> {
    error("ConversationHostConfiguration must be provided by the host.")
}
