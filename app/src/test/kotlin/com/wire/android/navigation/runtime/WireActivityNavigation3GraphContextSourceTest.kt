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

class WireActivityNavigation3GraphContextSourceTest {

    @Test
    fun givenNavigation3GraphContext_whenInspectingSource_thenItUsesTypedResolverOnly() {
        val source = sourceFile().readText()
        val function = source.substringAfter("fun rememberWireNavigation3ActivityGraphContext")
            .substringBefore("@Composable\ninternal fun rememberWireActivityGraphContext")

        assertTrue("graphResolver.resolve(route)" in function)
        listOf(
            "Bundle",
            "NavController",
            "com.ramcosta",
            "LegacyWireActivityRouteClassifier",
            "currentBackStackEntry",
        ).forEach { forbidden -> assertFalse(forbidden in function, forbidden) }
    }

    private fun sourceFile(): File {
        val relative = "src/main/kotlin/com/wire/android/navigation/runtime/WireActivityGraphContext.kt"
        return sequenceOf(
            File(relative),
            File("app/$relative"),
            File("../app/$relative"),
        ).first(File::isFile)
    }
}
