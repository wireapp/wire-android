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
package com.wire.android.ui.home.conversationslist.common

import androidx.compose.runtime.compositionLocalOf
import com.wire.android.feature.privacy.model.EffectivePrivacyLevel
import com.wire.kalium.logic.data.id.ConversationId

/**
 * Provides the per-conversation [EffectivePrivacyLevel] map down to conversation rows without
 * threading it through the paged-list plumbing. Defaults to an empty map (everything NORMAL), so
 * rows render normally wherever the provider is absent.
 */
val LocalConversationEffectivePrivacy = compositionLocalOf { emptyMap<ConversationId, EffectivePrivacyLevel>() }
