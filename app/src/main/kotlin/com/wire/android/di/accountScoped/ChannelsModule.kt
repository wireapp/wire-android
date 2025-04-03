/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.di.accountScoped

import com.wire.android.di.CurrentAccount
import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.channels.ChannelsScope
import com.wire.kalium.logic.feature.conversation.channel.IsSelfEligibleToAddParticipantsToChannelUseCase
import com.wire.kalium.logic.feature.conversation.channel.UpdateChannelAddPermissionUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
class ChannelsModule {

    @Provides
    @ViewModelScoped
    fun provideChannelsScope(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ChannelsScope = coreLogic.getSessionScope(currentAccount).channels

    @ViewModelScoped
    @Provides
    fun provideUpdateChannelAddPermission(channelsScope: ChannelsScope): UpdateChannelAddPermissionUseCase =
        channelsScope.updateChannelAddPermission

    @ViewModelScoped
    @Provides
    fun provideIsSelfEligibleToAddParticipantsToChannel(channelsScope: ChannelsScope): IsSelfEligibleToAddParticipantsToChannelUseCase =
        channelsScope.isSelfEligibleToAddParticipantsToChannel
}
