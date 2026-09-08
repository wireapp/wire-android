/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.navigation.runtime

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class Navigation3RecreationResultOwnershipSourceTest {

    @Test
    fun givenFolderAndDrawingResults_whenInspectingEntries_thenRequestIdsAreSaveableAndLocallyConsumed() {
        val home = source("ui/home/HomeNavigation3Entry.kt")
        val conversation = source("ui/home/conversations/ConversationNavigation3Entries.kt")
        val details = source(
            "ui/home/conversations/details/ConversationDetailsNavigation3Entries.kt"
        )
        val profile = source("ui/userprofile/UserProfileNavigation3Entries.kt")

        assertSaveableResult(home, "folderRequestIdValue", "ConversationFoldersNavigation3ResultType")
        assertSaveableResult(conversation, "drawingRequestId", "DrawingCanvasNavigation3ResultType")
        assertSaveableResult(details, "folderRequest", "ConversationFoldersNavigation3ResultType")
        assertSaveableResult(profile, "folderRequestIdValue", "ConversationFoldersNavigation3ResultType")
    }

    @Test
    fun givenProductionActions_whenInspectingSource_thenCallbacksAreNotUsedAsResultPersistence() {
        val source = source("navigation/runtime/WireNavigation3ProductionActions.kt")

        assertFalse("PendingFolderResult" in source)
        assertFalse("PendingDrawingResult" in source)
        assertFalse("pendingFolderResults" in source)
        assertFalse("pendingDrawingResults" in source)
        assertFalse("folderResultRevision" in source)
        assertFalse("drawingResultRevision" in source)
    }

    private fun assertSaveableResult(
        source: String,
        requestIdName: String,
        resultTypeName: String,
    ) {
        assertTrue("rememberSaveable" in source)
        assertTrue(requestIdName in source)
        assertTrue(resultTypeName in source)
        assertTrue("runtime.navigateForResult(" in source)
        assertTrue("runtime.consumeResult(" in source)
        assertTrue("WireNavResultRequestId" in source)
    }

    private fun source(path: String): String {
        val root = generateSequence(File(checkNotNull(System.getProperty("user.dir")))) { it.parentFile }
            .first { File(it, "app/src/main/kotlin").isDirectory }
        return File(root, "app/src/main/kotlin/com/wire/android/$path").readText()
    }
}
