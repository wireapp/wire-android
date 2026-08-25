/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.login

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CoordinatorOwnershipTest {
    @Test
    fun `verification selection is a feature policy`() {
        assertTrue(loginSurface(true) == LoginSurface.Verification)
        assertTrue(loginSurface(false) == LoginSurface.Main)
    }

    @Test
    fun `feature coordinators remain host independent`() {
        val root = repositoryRoot()
        val feature = root.resolve("features/authentication/src/main/kotlin/com/wire/android/ui")
        listOf(
            "authentication/welcome/WelcomeCoordinator.kt",
            "authentication/login/email/LoginEmailEffects.kt",
            "authentication/login/sso/LoginSSOEffects.kt",
            "newauthentication/login/NewLoginPresentation.kt",
        ).forEach { relative ->
            val source = Files.readString(feature.resolve(relative))
            assertTrue(source.contains("package com.wire.android.ui"))
            assertFalse(source.contains("com.wire.kalium"))
            assertFalse(source.contains("BuildConfig"))
            assertFalse(source.contains("CustomTabsHelper"))
        }
    }

    private fun repositoryRoot(): Path =
        generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("app/src/main/kotlin")) }
}
