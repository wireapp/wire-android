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

package com.wire.android.navigation.routes.auth

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuthenticationRouteSourcePurityTest {

    @Test
    fun givenKmpReadyAuthenticationContracts_whenInspectingImports_thenOnlyCommonDependenciesAreUsed() {
        contractSources.forEach { relativePath ->
            val source = Files.readAllLines(Path.of("src/main/kotlin").resolve(relativePath))
            val unsupportedImports = source
                .filter { it.startsWith("import ") }
                .map { it.removePrefix("import ").substringBefore(" as ") }
                .filterNot { importedType ->
                    importedType.startsWith("kotlin.") ||
                        importedType.startsWith("kotlinx.serialization.") ||
                        importedType.startsWith("com.wire.navigation.")
                }

            assertTrue(
                unsupportedImports.isEmpty(),
                "$relativePath has platform or implementation imports: $unsupportedImports",
            )
        }
    }

    private companion object {
        val contractSources = listOf(
            "com/wire/android/navigation/routes/auth/AuthenticationRoutes.kt",
            "com/wire/android/navigation/routes/auth/CreateAccountNavigation3.kt",
            "com/wire/android/navigation/routes/utility/InitialSyncRoute.kt",
            "com/wire/android/ui/authentication/devices/register/RegisterDeviceRoute.kt",
            "com/wire/android/ui/authentication/devices/remove/RemoveDeviceRoute.kt",
            "com/wire/android/ui/e2eiEnrollment/E2EIEnrollmentRoute.kt",
        )
    }
}
