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

import androidx.lifecycle.ViewModel
import com.wire.android.feature.cells.ui.CellsViewModelFactory
import com.wire.android.feature.cells.ui.CellsViewModelGraph
import com.wire.android.feature.meetings.ui.MeetingsViewModelFactory
import com.wire.android.feature.meetings.ui.MeetingsViewModelGraph
import com.wire.android.model.ImageAssetViewModelFactory
import com.wire.android.model.ImageAssetViewModelGraph
import com.wire.android.ui.authentication.AuthenticationViewModelFactory
import com.wire.android.ui.authentication.AuthenticationViewModelGraph
import com.wire.android.ui.calling.CallingViewModelFactory
import com.wire.android.ui.calling.CallingViewModelGraph
import com.wire.android.ui.debug.DebugInfoViewModelFactory
import com.wire.android.ui.debug.DebugInfoViewModelGraph
import com.wire.android.ui.home.HomeViewModelFactory
import com.wire.android.ui.home.HomeViewModelGraph
import com.wire.android.ui.home.conversations.ConversationCoreViewModelFactory
import com.wire.android.ui.home.conversations.ConversationCoreViewModelGraph
import com.wire.android.ui.home.conversations.ScopedMessageViewModelFactory
import com.wire.android.ui.home.conversations.ScopedMessageViewModelGraph
import com.wire.android.ui.home.settings.SettingsViewModelFactory
import com.wire.android.ui.home.settings.SettingsViewModelGraph
import com.wire.android.util.ui.WireSessionImageLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import javax.inject.Provider

/**
 * Android-only bridge for feature ViewModel factories while feature UI is being decoupled from Hilt call sites.
 */
@HiltViewModel
class WireActivityViewModelGraphBridge @Inject constructor(
    imageLoader: Provider<WireSessionImageLoader>,
    private val cellsViewModelFactoryProvider: Provider<CellsViewModelFactory>,
    private val authenticationViewModelFactoryProvider: Provider<AuthenticationViewModelFactory>,
    private val callingViewModelFactoryProvider: Provider<CallingViewModelFactory>,
    private val debugInfoViewModelFactoryProvider: Provider<DebugInfoViewModelFactory>,
    private val homeViewModelFactoryProvider: Provider<HomeViewModelFactory>,
    private val settingsViewModelFactoryProvider: Provider<SettingsViewModelFactory>,
    private val conversationCoreViewModelFactoryProvider: Provider<ConversationCoreViewModelFactory>,
    private val meetingsViewModelFactoryProvider: Provider<MeetingsViewModelFactory>,
    private val scopedMessageViewModelFactoryProvider: Provider<ScopedMessageViewModelFactory>,
) : ViewModel(),
    ImageAssetViewModelGraph,
    CellsViewModelGraph,
    AuthenticationViewModelGraph,
    CallingViewModelGraph,
    DebugInfoViewModelGraph,
    HomeViewModelGraph,
    SettingsViewModelGraph,
    ConversationCoreViewModelGraph,
    MeetingsViewModelGraph,
    ScopedMessageViewModelGraph {
    override val imageAssetViewModelFactory: ImageAssetViewModelFactory =
        ImageAssetViewModelFactory(imageLoader = imageLoader::get)

    override val cellsViewModelFactory: CellsViewModelFactory
        get() = cellsViewModelFactoryProvider.get()

    override val authenticationViewModelFactory: AuthenticationViewModelFactory
        get() = authenticationViewModelFactoryProvider.get()

    override val callingViewModelFactory: CallingViewModelFactory
        get() = callingViewModelFactoryProvider.get()

    override val debugInfoViewModelFactory: DebugInfoViewModelFactory
        get() = debugInfoViewModelFactoryProvider.get()

    override val homeViewModelFactory: HomeViewModelFactory
        get() = homeViewModelFactoryProvider.get()

    override val settingsViewModelFactory: SettingsViewModelFactory
        get() = settingsViewModelFactoryProvider.get()

    override val conversationCoreViewModelFactory: ConversationCoreViewModelFactory
        get() = conversationCoreViewModelFactoryProvider.get()

    override val meetingsViewModelFactory: MeetingsViewModelFactory
        get() = meetingsViewModelFactoryProvider.get()

    override val scopedMessageViewModelFactory: ScopedMessageViewModelFactory
        get() = scopedMessageViewModelFactoryProvider.get()
}
