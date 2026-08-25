/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.ui.authentication

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuthenticationViewModelOwnershipSourceTest {

    @Test
    fun givenAuthenticationViewModels_whenInspectingCreation_thenOnlyWireMetroGatewayDefinesIdentity() {
        val source = authenticationViewModelGraph().readText()

        assertTrue(source.contains("wireMetroViewModel"))
        assertTrue(source.contains("wireAssistedMetroViewModel"))
        listOf(
            "viewModelStoreOwner.hashCode()",
            "LocalWireViewModelScopeKey",
            "sessionKeyedMetroViewModelKey",
            "assistedMetroViewModel<",
            "metroxViewModel(",
            "navArgs.toString()",
            "loginNavArgs.toString()",
        ).forEach { forbidden ->
            assertFalse(source.contains(forbidden), "Authentication VM identity contains $forbidden")
        }
    }

    private fun authenticationViewModelGraph(): File {
        val userDir = requireNotNull(System.getProperty("user.dir"))
        val root = generateSequence(File(userDir)) { it.parentFile }
            .first { File(it, "app/src/main/kotlin").isDirectory }
        return File(
            root,
            "app/src/main/kotlin/com/wire/android/ui/authentication/AuthenticationViewModelGraph.kt",
        ).also {
            assertTrue(it.isFile, "Missing ${it.path}")
        }
    }
}
