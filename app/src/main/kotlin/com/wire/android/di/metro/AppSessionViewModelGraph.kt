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
import com.wire.android.feature.cells.ui.CellsMetroViewModelBindings
import com.wire.android.feature.meetings.ui.MeetingsManualViewModelFactoryMetroBindings
import com.wire.android.feature.meetings.ui.MeetingsMetroViewModelBindings
import com.wire.android.feature.sketch.SketchMetroViewModelBindings
import com.wire.android.mediaplayer.MediaPlayerManualViewModelFactoryMetroBindings
import com.wire.android.pdfviewer.PdfViewerManualViewModelFactoryMetroBindings
import com.wire.android.search.SearchManualViewModelFactoryMetroBindings
import com.wire.android.ui.authentication.AuthenticationViewModelGraph
import com.wire.android.ui.calling.CallingMetroViewModelBindings
import com.wire.android.ui.calling.CallingManualViewModelFactoryMetroBindings
import com.wire.android.ui.common.CommonMetroViewModelBindings
import com.wire.android.ui.common.CommonManualViewModelFactoryMetroBindings
import com.wire.android.ui.common.CoreUICommonManualViewModelFactoryMetroBindings
import com.wire.android.ui.debug.DebugMetroViewModelBindings
import com.wire.android.ui.debug.DebugInfoManualViewModelFactoryMetroBindings
import com.wire.android.ui.debug.DebugInfoViewModelGraph
import com.wire.android.ui.home.HomeMetroViewModelBindings
import com.wire.android.ui.home.HomeViewModelGraph
import com.wire.android.ui.home.conversations.ConversationSearchFolderMetroViewModelBindings
import com.wire.android.ui.home.conversations.ConversationCoreManualViewModelFactoryMetroBindings
import com.wire.android.ui.home.conversations.ConversationDetailsManualViewModelFactoryMetroBindings
import com.wire.android.ui.home.conversations.ConversationSearchFolderManualViewModelFactoryMetroBindings
import com.wire.android.ui.home.settings.SettingsManualViewModelFactoryMetroBindings
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.user.UserId
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Scope
import dev.zacsweers.metro.asContribution
import dev.zacsweers.metrox.viewmodel.ViewModelGraph

@Scope
annotation class MetroSessionScope

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
        WireMetroViewModelBindings::class,
        DebugInfoManualViewModelFactoryMetroBindings::class,
        DebugMetroViewModelBindings::class,
        HomeMetroViewModelBindings::class,
        ConversationSearchFolderMetroViewModelBindings::class,
        ConversationSearchFolderManualViewModelFactoryMetroBindings::class,
        ConversationCoreManualViewModelFactoryMetroBindings::class,
        ConversationDetailsManualViewModelFactoryMetroBindings::class,
        SettingsManualViewModelFactoryMetroBindings::class,
        CallingManualViewModelFactoryMetroBindings::class,
        CallingMetroViewModelBindings::class,
        CommonManualViewModelFactoryMetroBindings::class,
        CommonMetroViewModelBindings::class,
        CellsMetroViewModelBindings::class,
        MeetingsMetroViewModelBindings::class,
        MeetingsManualViewModelFactoryMetroBindings::class,
        SketchMetroViewModelBindings::class,
        CoreUICommonManualViewModelFactoryMetroBindings::class,
        SearchManualViewModelFactoryMetroBindings::class,
        MediaPlayerManualViewModelFactoryMetroBindings::class,
        PdfViewerManualViewModelFactoryMetroBindings::class,
        ImageLoadingModule::class,
    ]
)
interface AppSessionViewModelGraph :
    ViewModelGraph,
    AuthenticationViewModelGraph,
    DebugInfoViewModelGraph,
    HomeViewModelGraph {
    @get:CurrentAccount
    val currentAccount: UserId

    override val viewModelScopeKey: String
        get() = currentAccount.toString()

    val wireSessionImageLoader: WireSessionImageLoader

    @ContributesTo(AppScope::class)
    @GraphExtension.Factory
    interface Factory {
        fun createAppSessionViewModelGraph(@Provides @CurrentAccount currentAccount: UserId): AppSessionViewModelGraph
    }
}

fun WireApplicationGraph.createSessionViewModelGraph(currentAccount: UserId): AppSessionViewModelGraph {
    return asContribution<AppSessionViewModelGraph.Factory>().createAppSessionViewModelGraph(currentAccount)
}
