package com.wire.android.ui.authentication.create.overview

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreateAccountOverviewOwnershipTest {
    @Test
    fun `feature owns overview rendering and app only adapts host effects`() {
        val feature = source("features/authentication/src/main/kotlin/com/wire/android/ui/authentication/create/overview")
        val app = source("app/src/main/kotlin/com/wire/android/ui/authentication/create/overview/CreatePersonalAccountOverviewScreen.kt")
        assertTrue(feature.contains("fun CreateAccountOverviewContent("))
        assertTrue(feature.contains("class CreateAccountOverviewViewModel<LinksT>"))
        assertFalse(feature.contains("CreateAccountNavArgs"))
        assertTrue(app.contains("CustomTabsHelper.launchUrl"))
        assertTrue(app.contains("CreateAccountRouteFlowType"))
    }

    private fun source(path: String): String = File(root(), path).walkTopDown().filter(File::isFile).joinToString("\n") { it.readText() }
    private fun root(): File = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
        .first { File(it, "app/src/main/kotlin").isDirectory }
}
