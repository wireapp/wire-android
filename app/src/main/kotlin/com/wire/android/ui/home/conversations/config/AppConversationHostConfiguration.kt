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

package com.wire.android.ui.home.conversations.config

import com.wire.android.BuildConfig
import com.wire.android.feature.conversation.config.ConversationHostConfiguration
import com.wire.android.feature.conversation.config.ConversationRuntimeCapabilities
import com.wire.android.feature.conversation.config.ConversationUiVisibility
import com.wire.android.util.debug.FeatureVisibilityFlags

object AppConversationHostConfiguration : ConversationHostConfiguration {
    override val runtime = ConversationRuntimeCapabilities(
        bubbleUiEnabled = BuildConfig.IS_BUBBLE_UI_ENABLED,
        pendingMessagesEnabled = BuildConfig.PENDING_MESSAGES,
        developerFeaturesEnabled = BuildConfig.DEVELOPER_FEATURES_ENABLED,
        mlsReadReceiptsEnabled = BuildConfig.MLS_READ_RECEIPTS_ENABLED,
        privateBuild = BuildConfig.PRIVATE_BUILD,
        passwordProtectedGuestLinksEnabled = BuildConfig.IS_PASSWORD_PROTECTED_GUEST_LINK_ENABLED,
    )

    override val visibility = ConversationUiVisibility(
        audioMessages = FeatureVisibilityFlags.AudioMessagesIcon,
        shareLocation = FeatureVisibilityFlags.ShareLocationIcon,
        drawing = FeatureVisibilityFlags.DrawingIcon,
        emoji = FeatureVisibilityFlags.EmojiIcon,
        gif = FeatureVisibilityFlags.GifIcon,
        ping = FeatureVisibilityFlags.PingIcon,
        topBarConversationSearch = FeatureVisibilityFlags.ConversationSearchIcon,
        messageSearch = FeatureVisibilityFlags.SearchConversationMessages,
    )
}
