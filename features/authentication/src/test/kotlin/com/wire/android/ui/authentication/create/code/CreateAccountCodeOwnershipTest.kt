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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreateAccountCodeOwnershipTest {
    @Test
    fun givenCodeStateEngine_thenFeatureOwnsViewModelStateGatewayAndResult() {
        CreateAccountCodeOwnershipFixtures.sourceFiles.forEach { source ->
            assertFalse(Files.exists(CreateAccountCodeOwnershipFixtures.appSource(source)))
            assertTrue(Files.isRegularFile(CreateAccountCodeOwnershipFixtures.featureSource(source)))
        }
        assertTrue(
            Files.isRegularFile(
                CreateAccountCodeOwnershipFixtures.appSource("CreateAccountCodeViewModelHostFactory.kt"),
            ),
        )
        assertFalse(
            Files.exists(
                CreateAccountCodeOwnershipFixtures.root.resolve(
                    "app/src/main/kotlin/com/wire/android/ui/registration/code/CreateAccountVerificationCodeViewState.kt",
                ),
            ),
        )
    }

    @Test
    fun givenFeatureCodeSources_thenHostTypesAndResourcesDoNotCrossBoundary() {
        val source = CreateAccountCodeOwnershipFixtures.featureCodeSource()

        CreateAccountCodeOwnershipFixtures.forbidden.forEach { value ->
            assertFalse(source.contains(value), "Feature code source contains $value")
        }
        assertTrue(source.contains("class CreateAccountCodeViewModel<FlowT, LinksT, FailureT, UserT, CredentialsT>"))
        assertTrue(source.contains("const val RESEND_TIMER_DELAY = 300L"))
        assertTrue(source.contains("val codeLength: Int = DEFAULT_VERIFICATION_CODE_LENGTH"))
    }

    @Test
    fun givenFeatureCodeSources_whenInspectingImports_thenAppAndHostDependenciesAreExcluded() {
        val imports = CreateAccountCodeOwnershipFixtures.featureImports()

        CreateAccountCodeOwnershipFixtures.forbiddenImports.forEach { forbiddenImport ->
            assertFalse(
                imports.any { it.contains(forbiddenImport) },
                "Forbidden feature dependency: $forbiddenImport",
            )
        }
    }
}
