/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.authentication.login

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LoginStateOwnershipTest {

    @Test
    fun givenLoginState_thenFeatureOwnsGenericContract() {
        val root = repositoryRoot()
        assertFalse(Files.exists(root.resolve("app/src/main/kotlin/$packagePath/LoginState.kt")))

        val source = Files.readString(root.resolve("features/authentication/src/main/kotlin/$packagePath/LoginState.kt"))
        assertTrue(source.contains("sealed class LoginState<out FailureT, out UserT, out SsoFailureT>"))
        assertTrue(source.contains("data class GenericError<out FailureT>(val coreFailure: FailureT)"))
        assertTrue(source.contains("data class SSOResultError<out SsoFailureT>(val result: SsoFailureT)"))
        assertTrue(source.contains("data class TooManyDevicesError<out UserT>(val userId: UserT)"))
        forbiddenDependencies.forEach { forbidden ->
            assertFalse(source.contains(forbidden), "Forbidden feature dependency: $forbidden")
        }
    }

    private fun repositoryRoot(): Path =
        generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("app/src/main/kotlin")) }

    private companion object {
        const val packagePath = "com/wire/android/ui/authentication/login"
        val forbiddenDependencies = listOf(
            "com.wire.kalium",
            "com.wire.android.util.deeplink",
            "CoreFailure",
            "UserId",
            "SSOFailureCodes",
        )
    }
}
