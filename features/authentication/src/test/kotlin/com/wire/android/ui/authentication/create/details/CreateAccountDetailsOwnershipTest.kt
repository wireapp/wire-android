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
        assertTrue(feature.contains("testTag(\"firstName\")"))
        assertTrue(feature.contains("testTag(\"teamName\")"))
        assertTrue(feature.contains("WirePasswordTextField("))
        assertTrue(feature.contains("genericFailureContent(dialogError.coreFailure, onErrorDismiss)"))
        assertFalse(feature.contains("CreateAccountNavArgs"))
        assertFalse(feature.contains("com.wire.kalium"))
        assertFalse(feature.contains("CoreFailureErrorDialog"))
        assertTrue(app.contains("CreateAccountCodeRoute("))
        assertTrue(app.contains("showTeamName = policy.isTeam"))
        assertTrue(app.contains("firstName = firstNameTextState.text.toString().trim()"))
        assertTrue(app.contains("password = passwordTextState.text.toString()"))
    }

    private fun source(path: String): String = File(root(), path).walkTopDown().filter(File::isFile).joinToString("\n") {
        it.readText()
    }
    private fun root(): File = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
        .first { File(it, "app/src/main/kotlin").isDirectory }
}
