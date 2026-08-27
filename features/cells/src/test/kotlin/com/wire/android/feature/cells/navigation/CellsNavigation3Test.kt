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

import com.wire.android.feature.cells.ui.CellFilesNavArgs
import com.wire.android.feature.cells.ui.search.DriveSearchScreenType
import com.wire.android.feature.cells.ui.search.sort.SortCriteriaNavArg
import com.wire.navigation.WireSessionId
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CellsNavigation3Test {

    @Test
    fun givenCellsRoutes_whenRouteIdsAreCollected_thenEveryRegistrationIsUnique() {
        val sessionId = WireSessionId("session", "wire.com")
        val routes = listOf(
            ConversationFilesRoute(sessionId, CellsFilesArguments()),
            ConversationFilesSlideRoute(sessionId, CellsFilesArguments()),
            RecycleBinRoute(sessionId, CellsFilesArguments(isRecycleBin = true)),
            CreateFolderRoute(sessionId, null),
            CreateFileRoute(sessionId, "parent", CellsFileType.DOCUMENT),
            MoveToFolderRoute(sessionId, "current", "node", "uuid"),
            PublicLinkRoute(sessionId, "asset", "name", null, false),
            PublicLinkExpirationRoute(sessionId, "link", null),
            PublicLinkPasswordRoute(sessionId, "link", false),
            RenameNodeRoute(sessionId, "uuid", "path", false, "name"),
            AddRemoveTagsRoute(sessionId, "uuid", emptyList()),
            VersionHistoryRoute(sessionId, "uuid", "name"),
            CellImageViewerRoute(sessionId),
            VideoPlayerRoute(sessionId),
            AudioPlayerRoute(sessionId),
            SearchRoute(sessionId),
        )

        assertEquals(CellsNavigation3Contribution.ROUTE_REGISTRATION_COUNT, routes.size)
        assertEquals(routes.size, routes.map { it.routeId }.distinct().size)
        assertTrue(routes.all { it.sessionId == sessionId })
    }

    @Test
    fun givenCellsResultTypes_whenCollected_thenContractsAreUnique() {
        val ids = CellsNavigation3Contribution.resultTypes.map { it.contract.id }

        assertEquals(ids.size, ids.distinct().size)
        assertEquals(2, ids.size)
    }

    @Test
    fun givenScreenFilesArguments_whenMappedRoundTrip_thenValuesArePreserved() {
        val screenArgs = CellFilesNavArgs(
            conversationId = "id@wire.com",
            screenTitle = "Title / with spaces",
            isRecycleBin = true,
            breadcrumbs = arrayOf("root", "A/B"),
            parentFolderUuid = null,
            isSearchByDefaultActive = true,
        )

        assertEquals(screenArgs, screenArgs.toNavigation3().toScreenArgs())
    }

    @Test
    fun givenSearchRoute_whenMappedToScreenArgs_thenSortCriteriaIsPreserved() {
        val route = SearchRoute(
            sessionId = WireSessionId("session", "wire.com"),
            conversationId = "conversationUuid",
            sortCriteria = SortCriteriaNavArg.NameAZ,
            screenType = CellsSearchType.DRIVE,
            parentRoute = "parent",
        )

        val screenArgs = route.toScreenArgs()

        assertEquals("conversationUuid", screenArgs.conversationId)
        assertEquals(DriveSearchScreenType.DRIVE, screenArgs.screenType)
        assertEquals("parent", screenArgs.parentRoute)
        assertEquals(SortCriteriaNavArg.NameAZ, screenArgs.initialSortingCriteria)
    }

    @Test
    fun givenCellsResultRequests_whenInspectingRenderer_thenIdsSurviveRecreation() {
        val source = sourceFile().readText()

        assertTrue("rememberSaveable(route.entryId.value, \"boolean-result\")" in source)
        assertTrue("rememberSaveable(route.entryId.value, \"expiration-result\")" in source)
        assertTrue("requestIdValue: String?" in source)
        assertTrue("requestIdValue?.let(::WireNavResultRequestId)" in source)
        assertFalse("mutableStateOf<WireNavResultRequestId?>" in source)
    }

    private fun sourceFile(): File {
        val root = generateSequence(File(checkNotNull(System.getProperty("user.dir")))) { it.parentFile }
            .first { File(it, "features/cells/src/main").isDirectory }
        return File(
            root,
            "features/cells/src/main/java/com/wire/android/feature/cells/navigation/CellsNavigation3Renderer.kt",
        )
    }
}
