/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.feature.cells.navigation

import com.wire.android.navigation.navigation3.WireEntryPresentation
import com.wire.android.navigation.navigation3.WireEntryProviderInstaller
import com.wire.android.navigation.navigation3.WireNavigation3ResultType
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.navigation3.wireEntry

val CellsBooleanNavigation3ResultType: WireNavigation3ResultType<CellsBooleanResult> =
    WireNavigation3ResultType(
        CellsBooleanResultContract,
        CellsBooleanResult.serializer(),
    )

val PublicLinkExpirationNavigation3ResultType:
    WireNavigation3ResultType<PublicLinkExpirationNavigationResult> =
    WireNavigation3ResultType(
        PublicLinkExpirationResultContract,
        PublicLinkExpirationNavigationResult.serializer(),
    )

object CellsNavigation3Contribution {
    const val ROUTE_REGISTRATION_COUNT: Int = 17

    val resultTypes: List<WireNavigation3ResultType<*>> =
        listOf(
            CellsBooleanNavigation3ResultType,
            PublicLinkExpirationNavigation3ResultType,
        )

    fun entryProviderInstallers(
        runtime: WireNavigation3Runtime,
        onExitCells: () -> Unit,
    ): List<WireEntryProviderInstaller> =
        listOf(cellsNavigation3Entries(runtime, onExitCells))
}

internal fun cellsNavigation3Entries(
    runtime: WireNavigation3Runtime,
    onExitCells: () -> Unit,
): WireEntryProviderInstaller = {
    wireEntry<ConversationFilesRoute>(presentation = WireEntryPresentation.PopUp) {
        CellsNavigation3RouteScreen(it, runtime, onExitCells)
    }
    wireEntry<ConversationFilesSlideRoute> {
        CellsNavigation3RouteScreen(it, runtime, onExitCells)
    }
    wireEntry<RecycleBinRoute> {
        CellsNavigation3RouteScreen(it, runtime, onExitCells)
    }
    wireEntry<CreateFolderRoute>(presentation = WireEntryPresentation.PopUp) {
        CellsNavigation3RouteScreen(it, runtime, onExitCells)
    }
    wireEntry<CreateFileRoute>(presentation = WireEntryPresentation.PopUp) {
        CellsNavigation3RouteScreen(it, runtime, onExitCells)
    }
    wireEntry<MoveToFolderRoute> {
        CellsNavigation3RouteScreen(it, runtime, onExitCells)
    }
    wireEntry<PublicLinkRoute>(presentation = WireEntryPresentation.PopUp) {
        CellsNavigation3RouteScreen(it, runtime, onExitCells)
    }
    wireEntry<PublicLinkExpirationRoute> {
        CellsNavigation3RouteScreen(it, runtime, onExitCells)
    }
    wireEntry<PublicLinkPasswordRoute> {
        CellsNavigation3RouteScreen(it, runtime, onExitCells)
    }
    wireEntry<RenameNodeRoute>(presentation = WireEntryPresentation.PopUp) {
        CellsNavigation3RouteScreen(it, runtime, onExitCells)
    }
    wireEntry<AddRemoveTagsRoute>(presentation = WireEntryPresentation.PopUp) {
        CellsNavigation3RouteScreen(it, runtime, onExitCells)
    }
    wireEntry<VersionHistoryRoute>(presentation = WireEntryPresentation.PopUp) {
        CellsNavigation3RouteScreen(it, runtime, onExitCells)
    }
    wireEntry<CellImageViewerRoute>(presentation = WireEntryPresentation.PopUp) {
        CellsNavigation3RouteScreen(it, runtime, onExitCells)
    }
    wireEntry<VideoPlayerRoute>(presentation = WireEntryPresentation.PopUp) {
        CellsNavigation3RouteScreen(it, runtime, onExitCells)
    }
    wireEntry<AudioPlayerRoute>(presentation = WireEntryPresentation.PopUp) {
        CellsNavigation3RouteScreen(it, runtime, onExitCells)
    }
    wireEntry<PdfViewerRoute>(presentation = WireEntryPresentation.PopUp) {
        CellsNavigation3RouteScreen(it, runtime, onExitCells)
    }
    wireEntry<SearchRoute>(presentation = WireEntryPresentation.PopUp) {
        CellsNavigation3RouteScreen(it, runtime, onExitCells)
    }
}
