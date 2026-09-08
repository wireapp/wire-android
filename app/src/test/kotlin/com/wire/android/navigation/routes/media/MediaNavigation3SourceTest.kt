/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.navigation.routes.media

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MediaNavigation3SourceTest {
    @Test
    fun givenMediaContracts_whenInspectingImports_thenTheyStayKmpPure() {
        val source = source("navigation/routes/media/MediaRoutes.kt")
        listOf("android.", "androidx.", "com.ramcosta.", "com.wire.kalium.").forEach {
            assertFalse(source.lineSequence().any { line -> line.startsWith("import $it") })
        }
        listOf("Bundle", "SavedStateHandle", "Parcelable", "NavArgs").forEach {
            assertFalse(source.contains(it))
        }
    }

    @Test
    fun givenMediaEntries_whenInspectingSource_thenAllRoutesAndTypedFactoriesArePresent() {
        val source = source("navigation/routes/media/MediaNavigation3Entries.kt")
        listOf(
            "ConversationMediaRoute",
            "ImagesPreviewRoute",
            "MediaGalleryRoute",
            "MessageDetailsRoute",
            "LoggedOutImportMediaRoute",
            "AuthenticatedImportMediaRoute",
        ).forEach { assertTrue(source.contains("wireEntry<$it>")) }
        assertFalse(source.contains("ScreenDestination"))
        assertFalse(source.contains("SavedStateHandle"))
        assertFalse(source.contains("Bundle"))
        assertTrue(source.contains("toViewModelArgs()"))
    }

    private fun source(path: String): String {
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "app/src/main/kotlin").isDirectory }
        return File(root, "app/src/main/kotlin/com/wire/android/$path").readText()
    }
}
