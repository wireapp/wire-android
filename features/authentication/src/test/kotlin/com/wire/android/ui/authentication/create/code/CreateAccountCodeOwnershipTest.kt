/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.authentication.create.code

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreateAccountCodeOwnershipTest {
    @Test
    fun givenCodeStateEngine_thenFeatureOwnsViewModelStateGatewayAndResult() {
        val root = repositoryRoot()
        sourceFiles.forEach { source ->
            assertFalse(Files.exists(root.resolve("app/src/main/kotlin/$packagePath/$source")))
            assertTrue(Files.isRegularFile(root.resolve("features/authentication/src/main/kotlin/$packagePath/$source")))
        }
        assertTrue(Files.isRegularFile(root.resolve("app/src/main/kotlin/$packagePath/CreateAccountCodeViewModelHostFactory.kt")))
        assertTrue(Files.isRegularFile(root.resolve("app/src/main/kotlin/com/wire/android/ui/registration/code/CreateAccountVerificationCodeViewState.kt")))
    }

    @Test
    fun givenFeatureCodeSources_thenHostTypesAndResourcesDoNotCrossBoundary() {
        val root = repositoryRoot()
        val sourceRoot = root.resolve("features/authentication/src/main/kotlin/$packagePath")
        val source = Files.walk(sourceRoot).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".kt") }
                .map(Files::readString)
                .toList()
                .joinToString("\n")
        }
        forbidden.forEach { value -> assertFalse(source.contains(value), "Feature code source contains $value") }
        assertTrue(source.contains("class CreateAccountCodeViewModel<FlowT, LinksT, FailureT, UserT, CredentialsT>"))
        assertTrue(source.contains("const val RESEND_TIMER_DELAY = 300L"))
        assertTrue(source.contains("val codeLength: Int = DEFAULT_VERIFICATION_CODE_LENGTH"))
    }

    private fun repositoryRoot(): Path =
        generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("app/src/main/kotlin")) }

    private companion object {
        const val packagePath = "com/wire/android/ui/authentication/create/code"
        val sourceFiles = listOf("CreateAccountCodeViewModel.kt", "CreateAccountCodeViewState.kt", "CreateAccountCodeGateway.kt")
        val forbidden = listOf(
            "com.wire.kalium",
            "com.wire.android.BuildConfig",
            "CreateAccountNavArgs",
            "CreateAccountFlowType",
            "com.wire.kalium.logic.configuration.server.ServerConfig",
            "com.wire.kalium.logic.CoreLogic",
            "com.wire.kalium.logic.feature.register.RegisterParam",
            "com.wire.kalium.logic.data.session.StoreSessionParam",
            "com.wire.kalium.logic.feature.client.RegisterClientParam",
            "dev.zacsweers.metro",
            "com.wire.android.R",
            "com.wire.android.ui.registration.code.CreateAccountCodeResult",
        )
    }
}
