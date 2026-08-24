/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LegacyAuthenticationPresentationStructureTest {

    @Test
    fun `backend adapter delegates presentation and keeps platform tags`() {
        val source = appSource("BackendConfigSetup.kt")
        assertTrue(source.contains("BackendConfigFormContent("))
        assertTrue(source.contains("BackendConfigSuccessContent("))
        assertTrue(source.contains("backendConfigCameraButton"))
        assertTrue(source.contains("context::openBackendConfig"))
    }

    @Test
    fun `login button keeps wrapper and child tag boundary`() {
        val feature = featureSource("login/email/LoginEmailContent.kt")
        val app = appSource("login/email/LoginEmailScreen.kt")
        assertTrue(feature.contains("Column(modifier = modifier)"))
        assertTrue(feature.contains("modifier = Modifier.testTag(\"loginButton\")"))
        assertTrue(app.contains("fun LoginButton("))
        assertTrue(app.contains("loadingText: String = stringResource(R.string.label_logging_in)"))
    }

    @Test
    fun `welcome content owns close and accessibility presentation`() {
        val source = featureSource("welcome/WelcomeScreenContent.kt")
        assertTrue(source.contains("if (state.showCloseButton)"))
        assertTrue(source.contains("onNavigationPressed = onClose"))
        assertTrue(source.contains("testTagsAsResourceId = true"))
        assertTrue(source.contains("onClickLabel = openLinkDescription"))
        assertTrue(source.contains("overflow = TextOverflow.Ellipsis"))
    }

    private fun appSource(relativePath: String): String =
        Files.readString(repositoryRoot().resolve("app/src/main/kotlin/com/wire/android/ui/authentication/$relativePath"))

    private fun featureSource(relativePath: String): String =
        Files.readString(repositoryRoot().resolve("features/authentication/src/main/kotlin/com/wire/android/ui/authentication/$relativePath"))

    private fun repositoryRoot(): Path =
        generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("app/src/main/kotlin")) }
}
