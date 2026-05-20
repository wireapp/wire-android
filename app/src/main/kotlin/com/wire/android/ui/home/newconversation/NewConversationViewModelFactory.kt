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
package com.wire.android.ui.home.newconversation

import com.wire.kalium.logic.feature.channels.ObserveChannelsCreationPermissionUseCase
import com.wire.kalium.logic.feature.client.IsWireCellsEnabledUseCase
import com.wire.kalium.logic.feature.conversation.createconversation.CreateChannelUseCase
import com.wire.kalium.logic.feature.conversation.createconversation.CreateRegularGroupUseCase
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppsAllowedForUsageUseCase
import com.wire.kalium.logic.feature.user.GetDefaultProtocolUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import dev.zacsweers.metro.Inject

@Inject
@Suppress("LongParameterList")
class NewConversationViewModelFactory(
    private val createRegularGroup: CreateRegularGroupUseCase,
    private val createChannel: CreateChannelUseCase,
    private val isUserAllowedToCreateChannels: ObserveChannelsCreationPermissionUseCase,
    private val getSelfUser: GetSelfUserUseCase,
    private val getDefaultProtocol: GetDefaultProtocolUseCase,
    private val isWireCellsFeatureEnabled: IsWireCellsEnabledUseCase,
    private val observeIsAppsAllowedForUsage: ObserveIsAppsAllowedForUsageUseCase,
) {
    fun create(): NewConversationViewModel = NewConversationViewModel(
        createRegularGroup = createRegularGroup,
        createChannel = createChannel,
        isUserAllowedToCreateChannels = isUserAllowedToCreateChannels,
        getSelfUser = getSelfUser,
        getDefaultProtocol = getDefaultProtocol,
        isWireCellsFeatureEnabled = isWireCellsFeatureEnabled,
        observeIsAppsAllowedForUsage = observeIsAppsAllowedForUsage,
    )
}
