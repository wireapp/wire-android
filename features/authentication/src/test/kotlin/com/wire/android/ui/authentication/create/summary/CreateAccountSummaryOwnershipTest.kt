package com.wire.android.ui.authentication.create.summary

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreateAccountSummaryOwnershipTest {
    @Test
    fun `summary has no app parcel arguments and owns its stateless rendering`() {
        val root = root()
        val app = File(root, "app/src/main/kotlin/com/wire/android/ui/authentication/create/summary")
        val feature = File(
            root,
            "features/authentication/src/main/kotlin/com/wire/android/ui/authentication/create/summary/CreateAccountSummaryScreen.kt",
        )
        assertFalse(File(app, "CreateAccountSummaryNavArgs.kt").exists())
        val source = feature.readText()
        assertTrue(source.contains("fun CreateAccountSummaryRouteScreen("))
        assertTrue(source.contains("onContinuePressed = onContinue"))
        assertTrue(source.contains("WirePrimaryButton("))
        assertTrue(source.contains("CreateAccountRouteFlowType.PERSONAL"))
        assertTrue(source.contains("CreateAccountRouteFlowType.TEAM"))
        assertFalse(source.contains("CreateAccountSummaryNavArgs"))
        assertFalse(source.contains("com.wire.kalium"))
    }

    private fun root(): File = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
        .first { File(it, "app/src/main/kotlin").isDirectory }
}
