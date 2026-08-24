/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.newauthentication.login

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NewLoginOwnershipTest {
    @Test
    fun `feature owns the generic NewLogin engine without host dependencies`() {
        val root = repositoryRoot()
        val featureDirectory = root.resolve("features/authentication/src/main/kotlin/$packagePath")
        val source = Files.walk(featureDirectory).use { paths ->
            paths.filter { it.fileName.toString().endsWith(".kt") }
                .map(Files::readString)
                .toList()
                .joinToString("\n")
        }
        listOf("NewLoginViewModel.kt", "NewLoginScreenState.kt", "NewLoginFlowState.kt", "NewLoginAction.kt").forEach {
            assertFalse(Files.exists(root.resolve("app/src/main/kotlin/$packagePath/$it")), "Legacy app source remains: $it")
            assertTrue(Files.exists(featureDirectory.resolve(it)), "Missing feature source: $it")
        }
        assertTrue(Files.exists(featureDirectory.resolve("NewLoginContainer.kt")))
        assertTrue(Files.exists(featureDirectory.resolve("NewLoginContent.kt")))
        assertTrue(Files.exists(featureDirectory.resolve("code/NewLoginVerificationCodeContent.kt")))
        assertTrue(
            Files.exists(
                root.resolve("app/src/main/kotlin/com/wire/android/ui/newauthentication/code/NewLoginVerificationCodeScreen.kt")
            )
        )
        assertFalse(Files.exists(featureDirectory.resolve("code/NewLoginVerificationCodeScreen.kt")))
        assertTrue(Files.exists(featureDirectory.resolve("password/NewLoginPasswordContent.kt")))
        assertFalse(Files.exists(root.resolve("app/src/main/kotlin/$packagePath/NewLoginContainer.kt")))
        assertTrue(source.contains("Role.Button"), "Forgot-password action must retain button semantics")
        forbidden.forEach { assertFalse(source.contains(it), "Forbidden feature dependency: $it") }
    }

    private fun repositoryRoot(): Path =
        generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("app/src/main/kotlin")) }

    private companion object {
        const val packagePath = "com/wire/android/ui/newauthentication/login"
        val forbidden = listOf(
            "com.wire.kalium",
            "com.wire.android.BuildConfig",
            "com.wire.android.R",
            "com.wire.android.datastore",
            "com.wire.android.ui.WireActivity",
            "LoginNavArgs",
            "LoginPasswordPath",
            "DomainClaimedByOrg",
            "DeepLinkResult",
            "SSOUrlConfig",
            "com.wire.android.ui.authentication.serverconfig.ServerConfig",
            "com.wire.kalium.logic.configuration.server.ServerConfig",
            "CoreFailure",
            "UserId",
            "SavedStateHandle",
            "CoreLogic",
            "dev.zacsweers.metro",
            "BackendSelectorDropDown",
            "android.content.Intent",
            "android.net.Uri",
            "android.content.Context",
            "LocalContext",
            "CustomTabsHelper",
            "SupportUrlResolver",
            "DispatcherProvider",
        )
    }
}
