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
package com.wire.android.di.metro

import com.wire.android.di.CurrentAccount
import com.wire.android.di.ImageLoadingModule
import com.wire.android.di.SessionModule
import com.wire.android.di.accountScoped.AppsModule
import com.wire.android.di.accountScoped.AuthenticationModule
import com.wire.android.di.accountScoped.BackupModule
import com.wire.android.di.accountScoped.CallsModule
import com.wire.android.di.accountScoped.CellsModule
import com.wire.android.di.accountScoped.ChannelsModule
import com.wire.android.di.accountScoped.ClientModule
import com.wire.android.di.accountScoped.ConnectionModule
import com.wire.android.di.accountScoped.ConversationModule
import com.wire.android.di.accountScoped.DebugModule
import com.wire.android.di.accountScoped.MeetingModule
import com.wire.android.di.accountScoped.MessageModule
import com.wire.android.di.accountScoped.SearchModule
import com.wire.android.di.accountScoped.ServicesModule
import com.wire.android.di.accountScoped.TeamModule
import com.wire.android.di.accountScoped.UserModule
import com.wire.android.ui.authentication.AuthenticationViewModelGraph
import com.wire.android.ui.debug.DebugInfoViewModelGraph
import com.wire.android.ui.home.HomeViewModelGraph
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.UserSessionScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.asContribution
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory

@GraphExtension(
    MetroSessionScope::class,
    bindingContainers = [
        SessionModule::class,
        AppsModule::class,
        AuthenticationModule::class,
        BackupModule::class,
        CallsModule::class,
        CellsModule::class,
        ChannelsModule::class,
        ClientModule::class,
        ConnectionModule::class,
        ConversationModule::class,
        DebugModule::class,
        MessageModule::class,
        SearchModule::class,
        ServicesModule::class,
        TeamModule::class,
        UserModule::class,
        MeetingModule::class,
        ImageLoadingModule::class,
    ]
)
interface AppSessionViewModelGraph :
    AuthenticationViewModelGraph,
    DebugInfoViewModelGraph,
    HomeViewModelGraph {
    @get:CurrentAccount
    val currentAccount: UserId

    override val viewModelScopeKey: String
        get() = currentAccount.toString()

    val viewModelGraphFactory: AppViewModelGraph.Factory

    val metroViewModelFactory: MetroViewModelFactory

    val wireSessionImageLoader: WireSessionImageLoader

    @ContributesTo(AppScope::class)
    @GraphExtension.Factory
    interface Factory {
        fun createAppSessionViewModelGraph(
            @Provides @CurrentAccount currentAccount: UserId,
            @Provides session: UserSessionScope,
        ): AppSessionViewModelGraph
    }
}

fun WireApplicationGraph.createSessionViewModelGraph(currentAccount: UserId): AppSessionViewModelGraph {
    // Capture this generation now. A retained graph must never open a later same-account session.
    val session = coreLogic.getSessionScope(currentAccount)
    return asContribution<AppSessionViewModelGraph.Factory>().createAppSessionViewModelGraph(currentAccount, session)
}
