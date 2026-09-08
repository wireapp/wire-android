/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.di.metro

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WireMetroViewModelTest {

    @Test
    fun givenNoInstanceKey_whenBuildingViewModelKey_thenOnlyClassDefinesIdentity() {
        assertEquals(
            "com.wire.ExampleViewModel",
            wireViewModelInstanceKey("com.wire.ExampleViewModel"),
        )
    }

    @Test
    fun givenInstanceKey_whenBuildingViewModelKey_thenClassAndInstanceDefineIdentity() {
        assertEquals(
            "com.wire.ExampleViewModel:conversation-1",
            wireViewModelInstanceKey("com.wire.ExampleViewModel", "conversation-1"),
        )
    }

    @Test
    fun givenBlankClassName_whenBuildingViewModelKey_thenInputIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            wireViewModelInstanceKey("", "conversation-1")
        }
    }

    @Test
    fun givenCoexistenceRuntime_whenInspectingCreationCalls_thenOnlyNewAndLegacyGatewaysCallMetroXDirectly() {
        val metroDirectory = mainMetroDirectory()
        val directCreationPattern = Regex("""\b(?:assistedMetroViewModel|metroViewModel)\s*(?:<|\()""")
        val offenders = metroDirectory.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name !in ALLOWED_GATEWAY_FILES }
            .filter { directCreationPattern.containsMatchIn(it.readText()) }
            .map { it.name }
            .toList()

        assertTrue(
            offenders.isEmpty(),
            "Direct Metro ViewModel creation outside coexistence gateways $ALLOWED_GATEWAY_FILES: $offenders",
        )
    }

    @Test
    fun givenAssistedGateway_whenInspectingFactoryContract_thenCreationExtrasArePreserved() {
        val gateway = File(mainMetroDirectory(), GATEWAY_FILE).readText()

        assertTrue(gateway.contains("Factory.(CreationExtras) -> VM"))
        assertTrue(gateway.contains("create(extras)"))
    }

    private fun mainMetroDirectory(): File {
        return File(repositoryRoot(), "core/di/src/main/kotlin/com/wire/android/di/metro")
    }

    private fun repositoryRoot(): File {
        val userDirectory = checkNotNull(System.getProperty("user.dir"))
        return generateSequence(File(userDirectory)) { it.parentFile }
            .first { File(it, "core/di/src/main/kotlin").isDirectory }
    }

    private companion object {
        const val GATEWAY_FILE = "WireMetroViewModel.kt"
        val ALLOWED_GATEWAY_FILES = setOf(
            GATEWAY_FILE,
            // Kept deliberately until vertical migrations and the final cleanup branch.
            "MetroViewModelGraph.kt",
        )
    }
}
