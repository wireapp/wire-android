/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.authentication.login.email

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LoginEmailOwnershipTest {
    @Test
    fun `feature owns generic email engine state and contracts without host dependencies`() {
        val root = repositoryRoot()
        val featureDir = root.resolve("features/authentication/src/main/kotlin/$packagePath")
        val source = Files.list(featureDir).use { files ->
            files.filter { it.fileName.toString().endsWith(".kt") }.map(Files::readString).toList().joinToString("\n")
        }
        assertFalse(Files.exists(root.resolve("app/src/main/kotlin/$packagePath/LoginEmailViewModel.kt")))
        assertFalse(Files.exists(root.resolve("app/src/main/kotlin/$packagePath/LoginEmailState.kt")))
        assertTrue(source.contains("class LoginEmailViewModel<LinksT, FailureT, UserT, ScopeT, SessionT, BackendRequestT, DomainClaimT>"))
        assertTrue(source.contains("interface LoginEmailGateway<LinksT, FailureT, UserT, ScopeT, SessionT, BackendRequestT>"))
        forbidden.forEach { assertFalse(source.contains(it), "Forbidden feature dependency: $it") }
    }

    private fun repositoryRoot(): Path =
        generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("app/src/main/kotlin")) }

    private companion object {
        const val packagePath = "com/wire/android/ui/authentication/login/email"
        val forbidden = listOf(
            "com.wire.kalium",
            "com.wire.android.BuildConfig",
            "com.wire.android.datastore",
            "LoginNavArgs",
            "SavedStateHandle",
            "ServerConfig",
            "CoreLogic",
            "AuthenticationScope",
            "StoreSessionParam",
            "com.wire.kalium.logic.data.auth.ProxyCredentials",
            "dev.zacsweers.metro",
            "CustomTabsHelper",
            "SupportUrlResolver",
        )
    }
}
