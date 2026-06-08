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
import com.wire.android.feature.cells.ui.CellsViewModelFactory
import com.wire.android.feature.cells.ui.CellsViewModelGraph
import com.wire.android.feature.meetings.ui.MeetingsViewModelFactory
import com.wire.android.feature.meetings.ui.MeetingsViewModelGraph
import com.wire.android.model.ImageAssetViewModelFactory
import com.wire.android.model.ImageAssetViewModelGraph
import com.wire.android.ui.MiscViewModelFactory
import com.wire.android.ui.MiscViewModelGraph
import com.wire.android.ui.authentication.AuthenticationViewModelFactory
import com.wire.android.ui.authentication.AuthenticationViewModelGraph
import com.wire.android.ui.calling.CallingViewModelFactory
import com.wire.android.ui.calling.CallingViewModelGraph
import com.wire.android.ui.common.CommonViewModelFactory
import com.wire.android.ui.common.CommonViewModelGraph
import com.wire.android.ui.debug.DebugInfoViewModelFactory
import com.wire.android.ui.debug.DebugInfoViewModelGraph
import com.wire.android.ui.home.HomeViewModelFactory
import com.wire.android.ui.home.HomeViewModelGraph
import com.wire.android.ui.home.conversations.ConversationCoreViewModelFactory
import com.wire.android.ui.home.conversations.ConversationCoreViewModelGraph
import com.wire.android.ui.home.conversations.ConversationDetailsViewModelFactory
import com.wire.android.ui.home.conversations.ConversationDetailsViewModelGraph
import com.wire.android.ui.home.conversations.ConversationSearchFolderViewModelFactory
import com.wire.android.ui.home.conversations.ConversationSearchFolderViewModelGraph
import com.wire.android.ui.home.conversations.ScopedMessageViewModelFactory
import com.wire.android.ui.home.conversations.ScopedMessageViewModelGraph
import com.wire.android.ui.home.settings.SettingsViewModelFactory
import com.wire.android.ui.home.settings.SettingsViewModelGraph
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.user.UserId
import dev.zacsweers.metro.Inject

@Suppress("LongParameterList")
class AppSessionViewModelGraph @Inject constructor(
    @CurrentAccount currentAccount: UserId,
    imageLoader: () -> WireSessionImageLoader,
    private val cellsViewModelFactoryProvider: () -> CellsViewModelFactory,
    private val miscViewModelFactoryProvider: () -> MiscViewModelFactory,
    private val authenticationViewModelFactoryProvider: () -> AuthenticationViewModelFactory,
    private val callingViewModelFactoryProvider: () -> CallingViewModelFactory,
    private val debugInfoViewModelFactoryProvider: () -> DebugInfoViewModelFactory,
    private val homeViewModelFactoryProvider: () -> HomeViewModelFactory,
    private val settingsViewModelFactoryProvider: () -> SettingsViewModelFactory,
    private val conversationCoreViewModelFactoryProvider: () -> ConversationCoreViewModelFactory,
    private val conversationDetailsViewModelFactoryProvider: () -> ConversationDetailsViewModelFactory,
    private val conversationSearchFolderViewModelFactoryProvider: () -> ConversationSearchFolderViewModelFactory,
    private val meetingsViewModelFactoryProvider: () -> MeetingsViewModelFactory,
    private val scopedMessageViewModelFactoryProvider: () -> ScopedMessageViewModelFactory,
    private val commonViewModelFactoryProvider: () -> CommonViewModelFactory,
) : ImageAssetViewModelGraph,
    CellsViewModelGraph,
    MiscViewModelGraph,
    AuthenticationViewModelGraph,
    CallingViewModelGraph,
    DebugInfoViewModelGraph,
    HomeViewModelGraph,
    SettingsViewModelGraph,
    ConversationCoreViewModelGraph,
    ConversationDetailsViewModelGraph,
    ConversationSearchFolderViewModelGraph,
    MeetingsViewModelGraph,
    ScopedMessageViewModelGraph,
    CommonViewModelGraph {
    override val viewModelScopeKey: String = currentAccount.toString()

    override val imageAssetViewModelFactory: ImageAssetViewModelFactory =
        ImageAssetViewModelFactory(imageLoader = imageLoader)

    override val cellsViewModelFactory: CellsViewModelFactory
        get() = cellsViewModelFactoryProvider()

    override val miscViewModelFactory: MiscViewModelFactory
        get() = miscViewModelFactoryProvider()

    override val authenticationViewModelFactory: AuthenticationViewModelFactory
        get() = authenticationViewModelFactoryProvider()

    override val callingViewModelFactory: CallingViewModelFactory
        get() = callingViewModelFactoryProvider()

    override val debugInfoViewModelFactory: DebugInfoViewModelFactory
        get() = debugInfoViewModelFactoryProvider()

    override val homeViewModelFactory: HomeViewModelFactory
        get() = homeViewModelFactoryProvider()

    override val settingsViewModelFactory: SettingsViewModelFactory
        get() = settingsViewModelFactoryProvider()

    override val conversationCoreViewModelFactory: ConversationCoreViewModelFactory
        get() = conversationCoreViewModelFactoryProvider()

    override val conversationDetailsViewModelFactory: ConversationDetailsViewModelFactory
        get() = conversationDetailsViewModelFactoryProvider()

    override val conversationSearchFolderViewModelFactory: ConversationSearchFolderViewModelFactory
        get() = conversationSearchFolderViewModelFactoryProvider()

    override val meetingsViewModelFactory: MeetingsViewModelFactory
        get() = meetingsViewModelFactoryProvider()

    override val scopedMessageViewModelFactory: ScopedMessageViewModelFactory
        get() = scopedMessageViewModelFactoryProvider()

    override val commonViewModelFactory: CommonViewModelFactory
        get() = commonViewModelFactoryProvider()
}
