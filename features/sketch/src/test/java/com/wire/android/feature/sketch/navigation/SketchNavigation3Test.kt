/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.feature.sketch.navigation

import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SketchNavigation3Test {

    @Test
    fun givenDrawingRoute_whenReadingRouteId_thenLegacyIdentityIsPreserved() {
        assertEquals("sketch/drawing_canvas_screen", DrawingCanvasRoute.ROUTE_ID)
    }

    @Test
    fun givenDrawingRoute_whenSerializedAndRestored_thenArgumentsAndIdentityArePreserved() {
        val route = DrawingCanvasRoute(
            sessionId = WireSessionId("user", "wire.example"),
            conversationName = "Design",
            tempWritableUri = "content://wire/sketch.jpg",
            entryId = WireNavEntryId("drawing-entry"),
        )

        assertEquals(
            route,
            Json.decodeFromString<DrawingCanvasRoute>(Json.encodeToString(route)),
        )
    }

    @Test
    fun givenDrawingResult_whenSerializedAndRestored_thenUriIsPreservedAsPortableValue() {
        val result = DrawingCanvasResult("content://wire/sketch.jpg")

        assertEquals(
            result,
            Json.decodeFromString<DrawingCanvasResult>(Json.encodeToString(result)),
        )
    }

    @Test
    fun givenSketchEntry_whenInspectingSource_thenItUsesTypedResultAndPopUpPresentation() {
        val source = sourceFile("navigation/SketchNavigation3Entries.kt").readText()

        assertEquals(1, Regex("""wireEntry<""").findAll(source).count())
        assertTrue("WireEntryPresentation.PopUp" in source)
        assertTrue("WireNavResult.Canceled" in source)
        assertTrue("WireNavResult.Value" in source)
        listOf(
            "com.ramcosta",
            "NavController",
            "ResultBackNavigator",
            "SavedStateHandle",
        ).forEach { forbidden -> assertFalse(forbidden in source, forbidden) }
    }

    @Test
    fun givenSketchViewModelWiring_whenInspectingSource_thenMetroOwnsAPlatformNeutralViewModel() {
        val bindingSource = sourceFile("SketchMetroViewModelBindings.kt").readText()
        val viewModelSource = sourceFile("DrawingCanvasViewModel.kt").readText()

        assertTrue("@ViewModelKey(DrawingCanvasViewModel::class)" in bindingSource)
        assertTrue("fun drawingCanvasViewModel(): ViewModel" in bindingSource)
        listOf(
            "SavedStateHandle",
            "DrawingCanvasScreenDestination",
            "com.ramcosta",
        ).forEach { forbidden -> assertFalse(forbidden in viewModelSource, forbidden) }
    }

    private fun sourceFile(name: String): File {
        val relative = "src/main/java/com/wire/android/feature/sketch/$name"
        return sequenceOf(
            File(relative),
            File("features/sketch/$relative"),
            File("../features/sketch/$relative"),
        ).first(File::isFile)
    }
}
