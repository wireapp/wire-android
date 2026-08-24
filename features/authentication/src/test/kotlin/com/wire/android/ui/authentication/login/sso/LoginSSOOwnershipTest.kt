/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.authentication.login.sso

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LoginSSOOwnershipTest {
    @Test
    fun `feature owns generic SSO engine state gateway and saved input contract`() {
        val root = repositoryRoot()
        val featureDirectory = root.resolve("features/authentication/src/main/kotlin/$ssoPackage")
        val featureSource = Files.list(featureDirectory).use { files ->
            files.filter { it.fileName.toString().endsWith(".kt") }
                .map(Files::readString)
                .toList()
                .joinToString("\n")
        }
        assertFalse(Files.exists(root.resolve("app/src/main/kotlin/$ssoPackage/LoginSSOViewModel.kt")))
        assertFalse(Files.exists(root.resolve("app/src/main/kotlin/$ssoPackage/LoginSSOState.kt")))
        assertFalse(Files.exists(root.resolve("app/src/main/kotlin/$loginPackage/LoginSavedInputStore.kt")))
        assertTrue(featureSource.contains("class LoginSSOViewModel<LinksT, FailureT, UserT, SsoFailureT, SessionT>"))
        assertTrue(featureSource.contains("interface LoginSSOGateway<LinksT, FailureT, UserT, SessionT>"))
        assertTrue(Files.exists(root.resolve("features/authentication/src/main/kotlin/$loginPackage/LoginSavedInputStore.kt")))
        forbidden.forEach { assertFalse(featureSource.contains(it), "Forbidden feature dependency: $it") }
    }

    private fun repositoryRoot(): Path =
        generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("app/src/main/kotlin")) }

    private companion object {
        const val loginPackage = "com/wire/android/ui/authentication/login"
        const val ssoPackage = "$loginPackage/sso"
        val forbidden = listOf(
            "com.wire.kalium",
            "com.wire.android.BuildConfig",
            "com.wire.android.datastore",
            "LoginNavArgs",
            "SavedStateHandle",
            "DeepLinkResult",
            "com.wire.kalium.logic.configuration.server.ServerConfig",
            "CustomServerDetailsDialogState",
            "CoreLogic",
            "StoreSessionParam",
            "SQLiteException",
            "dev.zacsweers.metro",
        )
    }
}
