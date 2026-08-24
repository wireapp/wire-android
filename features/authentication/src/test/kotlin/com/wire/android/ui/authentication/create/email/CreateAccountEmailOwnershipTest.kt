package com.wire.android.ui.authentication.create.email

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreateAccountEmailOwnershipTest {
    @Test
    fun `email state and rendering stay feature-owned while host maps navigation`() {
        val feature = source("features/authentication/src/main/kotlin/com/wire/android/ui/authentication/create/email")
        val app = source("app/src/main/kotlin/com/wire/android/ui/authentication/create/email/CreateAccountEmailScreen.kt")
        assertTrue(feature.contains("class CreateAccountEmailViewModel<FlowT, LinksT, FailureT>"))
        assertTrue(feature.contains("fun <FlowT, FailureT> CreateAccountEmailContent("))
        assertFalse(feature.contains("CreateAccountNavArgs"))
        assertTrue(app.contains("CreateAccountDetailsRoute("))
        assertTrue(app.contains("CustomTabsHelper.launchUrl"))
    }

    private fun source(path: String): String = File(root(), path).walkTopDown().filter(File::isFile).joinToString("\n") { it.readText() }
    private fun root(): File = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
        .first { File(it, "app/src/main/kotlin").isDirectory }
}
