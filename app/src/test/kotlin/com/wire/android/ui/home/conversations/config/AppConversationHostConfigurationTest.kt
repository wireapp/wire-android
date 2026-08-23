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
import com.wire.android.di.KaliumConfigsModule
import com.wire.android.util.debug.FeatureVisibilityFlags
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppConversationHostConfigurationTest {

    @Test
    fun runtimeCapabilitiesMatchTheAppBuildConfiguration() {
        val runtime = AppConversationHostConfiguration.runtime

        assertEquals(BuildConfig.IS_BUBBLE_UI_ENABLED, runtime.bubbleUiEnabled)
        assertEquals(BuildConfig.PENDING_MESSAGES, runtime.pendingMessagesEnabled)
        assertEquals(BuildConfig.DEVELOPER_FEATURES_ENABLED, runtime.developerFeaturesEnabled)
        assertEquals(BuildConfig.MLS_READ_RECEIPTS_ENABLED, runtime.mlsReadReceiptsEnabled)
        assertEquals(BuildConfig.PRIVATE_BUILD, runtime.privateBuild)
        assertEquals(
            BuildConfig.IS_PASSWORD_PROTECTED_GUEST_LINK_ENABLED,
            runtime.passwordProtectedGuestLinksEnabled,
        )
    }

    @Test
    fun uiVisibilityMatchesTheExistingAppVisibilityFlags() {
        val visibility = AppConversationHostConfiguration.visibility

        assertEquals(FeatureVisibilityFlags.AudioMessagesIcon, visibility.audioMessages)
        assertEquals(FeatureVisibilityFlags.ShareLocationIcon, visibility.shareLocation)
        assertEquals(FeatureVisibilityFlags.DrawingIcon, visibility.drawing)
        assertEquals(FeatureVisibilityFlags.EmojiIcon, visibility.emoji)
        assertEquals(FeatureVisibilityFlags.GifIcon, visibility.gif)
        assertEquals(FeatureVisibilityFlags.PingIcon, visibility.ping)
        assertEquals(FeatureVisibilityFlags.ConversationSearchIcon, visibility.topBarConversationSearch)
        assertEquals(FeatureVisibilityFlags.SearchConversationMessages, visibility.messageSearch)
    }

    @Test
    fun pendingMessagesCapabilityIsSharedWithKaliumConfiguration() {
        assertEquals(
            AppConversationHostConfiguration.runtime.pendingMessagesEnabled,
            KaliumConfigsModule().provideKaliumConfigs().pendingMessages,
        )
    }
}
