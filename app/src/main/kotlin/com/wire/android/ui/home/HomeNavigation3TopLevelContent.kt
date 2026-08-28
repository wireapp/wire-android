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

@file:Suppress("MatchingDeclarationName")

package com.wire.android.ui.home

import androidx.compose.runtime.Composable
import com.wire.android.feature.cells.ui.AllFilesNavigationActions
import com.wire.android.feature.cells.ui.CellFilesNavArgs
import com.wire.android.feature.cells.ui.CellViewModel
import com.wire.android.feature.cells.ui.cellViewModel
import com.wire.android.feature.meetings.ui.MeetingsNavigationActions
import com.wire.android.navigation.HomeDestination
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.ui.home.archive.ArchiveScreen
import com.wire.android.ui.home.cell.GlobalCellsScreen
import com.wire.android.ui.home.conversationslist.ConversationsNavigationActions
import com.wire.android.ui.home.meetings.MeetingsScreen
import com.wire.android.ui.home.settings.SettingsNavigation3Actions
import com.wire.android.ui.home.settings.SettingsNavigation3Root
import com.wire.android.ui.home.vault.VaultScreen
import com.wire.android.ui.home.whatsnew.WhatsNewNavigation3Target
import com.wire.android.ui.home.whatsnew.WhatsNewScreen
import com.wire.navigation.WireSessionId

/**
 * Cross-batch actions emitted by Home top-level children.
 *
 * Generated directions and a Nav2 controller are intentionally excluded. The production host can
 * map these actions to typed feature routes as each owning batch lands.
 */
internal interface HomeTopLevelNavigation3Actions {
    val settings: SettingsNavigation3Actions

    /**
     * Temporary semantic boundary for Cells details whose typed contracts are owned by the Cells
     * migration: Search, Public Link, Add/Remove Tags, Image Viewer and Video Player.
     */
    val cells: AllFilesNavigationActions

    /** Navigation actions emitted by the Meetings top-level content. */
    val meetings: MeetingsNavigationActions

    /** Welcome/release-note detail contracts are migrated by the What's New detail batch. */
    fun openWhatsNew(target: WhatsNewNavigation3Target)
}

/**
 * Real top-level renderer for the Navigation 3 Home shell.
 *
 * Drawer selection remains state within Home rather than a second back-stack entry. Detail screens
 * opened from these roots are regular typed entries owned by their respective feature contribution.
 */
@Composable
internal fun HomeNavigation3TopLevelContent(
    destination: HomeTopLevelDestination,
    shellState: HomeShellState,
    sessionId: WireSessionId,
    runtime: WireNavigation3Runtime,
    actions: HomeTopLevelNavigation3Actions,
    conversationsNavigationActions: ConversationsNavigationActions,
) {
    when (destination) {
        HomeTopLevelDestination.CONVERSATIONS ->
            error("Conversations is rendered directly by HomeNavigation3Entry")

        HomeTopLevelDestination.SETTINGS -> SettingsNavigation3Root(
            sessionId = sessionId,
            runtime = runtime,
            actions = actions.settings,
            lazyListState = shellState.lazyListStateFor(HomeDestination.Settings),
        )

        HomeTopLevelDestination.VAULT -> VaultScreen()

        HomeTopLevelDestination.ARCHIVE -> ArchiveScreen(
            homeShellState = shellState,
            navigationActions = conversationsNavigationActions,
        )

        HomeTopLevelDestination.WHATS_NEW -> WhatsNewScreen(
            homeShellState = shellState,
            onOpenTarget = actions::openWhatsNew,
        )

        HomeTopLevelDestination.CELLS -> Navigation3GlobalCells(actions.cells)

        HomeTopLevelDestination.MEETINGS -> MeetingsScreen(
            homeShellState = shellState,
            navigationActions = actions.meetings,
        )
    }
}

@Suppress("ComposeViewModelForwarding")
@Composable
private fun Navigation3GlobalCells(
    navigationActions: AllFilesNavigationActions,
    viewModel: CellViewModel = cellViewModel(CellFilesNavArgs()),
) {
    GlobalCellsScreen(
        navigationActions = navigationActions,
        viewModel = viewModel,
    )
}
