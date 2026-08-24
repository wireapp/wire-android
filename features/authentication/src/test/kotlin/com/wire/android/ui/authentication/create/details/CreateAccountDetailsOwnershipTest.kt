package com.wire.android.ui.authentication.create.details

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreateAccountDetailsOwnershipTest {
    @Test
    fun `details state remains feature-owned and host emits the typed code route`() {
        val feature = source("features/authentication/src/main/kotlin/com/wire/android/ui/authentication/create/details")
        val app = source("app/src/main/kotlin/com/wire/android/ui/authentication/create/details/CreateAccountDetailsScreen.kt")
        assertTrue(feature.contains("class CreateAccountDetailsViewModel<LinksT, FailureT>"))
        assertTrue(feature.contains("fun <FailureT> CreateAccountDetailsContent("))
        assertFalse(feature.contains("CreateAccountNavArgs"))
        assertTrue(app.contains("CreateAccountCodeRoute("))
        assertTrue(app.contains("CreateAccountRouteFlowType.TEAM"))
    }

    private fun source(path: String): String = File(root(), path).walkTopDown().filter(File::isFile).joinToString("\n") { it.readText() }
    private fun root(): File = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
        .first { File(it, "app/src/main/kotlin").isDirectory }
}
