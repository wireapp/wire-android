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
package com.wire.android.ui.authentication.devices.remove

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RemoveDeviceOwnershipTest {

    @Test
    fun givenRemoveDeviceStateEngine_thenFeatureOwnsItAndAppOwnsOnlyHostAdapters() {
        val root = repositoryRoot()
        movedSourceNames.forEach { sourceName ->
            val relativePath = "$removePackagePath/$sourceName"
            assertFalse(Files.exists(root.resolve("app/src/main/kotlin/$relativePath")))
            assertTrue(Files.isRegularFile(root.resolve("features/authentication/src/main/kotlin/$relativePath")))
        }
        assertFalse(Files.exists(root.resolve("app/src/test/kotlin/$removePackagePath/RemoveDeviceViewModelTest.kt")))
        assertTrue(
            Files.isRegularFile(
                root.resolve("features/authentication/src/test/kotlin/$removePackagePath/RemoveDeviceViewModelTest.kt")
            )
        )
        assertTrue(Files.isRegularFile(root.resolve("app/src/main/kotlin/$removePackagePath/RemoveDeviceState.kt")))
        assertTrue(
            Files.isRegularFile(root.resolve("app/src/main/kotlin/$removePackagePath/RemoveDeviceGatewayAdapter.kt"))
        )
    }

    @Test
    fun givenFeatureRemoveDeviceSources_thenHostAndKaliumTypesDoNotCrossTheBoundary() {
        val root = repositoryRoot()
        val roots = listOf(
            root.resolve("features/authentication/src/main/kotlin/$removePackagePath"),
            root.resolve("features/authentication/src/test/kotlin/$removePackagePath"),
        )
        val imports = roots.flatMap { sourceRoot ->
            Files.walk(sourceRoot).use { paths ->
                paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".kt") }
                    .flatMap { Files.lines(it) }
                    .filter { it.startsWith("import ") }
                    .toList()
            }
        }

        forbiddenImportFragments.forEach { forbidden ->
            assertFalse(imports.any { it.contains(forbidden) }, "Forbidden feature dependency: $forbidden")
        }

        val viewModelSource = Files.readString(
            root.resolve("features/authentication/src/main/kotlin/$removePackagePath/RemoveDeviceViewModel.kt")
        )
        val gatewaySource = Files.readString(
            root.resolve("features/authentication/src/main/kotlin/$removePackagePath/RemoveDeviceGateway.kt")
        )
        assertTrue(viewModelSource.contains("class RemoveDeviceViewModel<DeviceT>"))
        assertTrue(viewModelSource.contains("REGISTER_CLIENT_AFTER_DELETE_DELAY_MILLIS = 2000L"))
        assertTrue(gatewaySource.contains("interface RemoveDeviceGateway<DeviceT> : RegisterDeviceGateway"))
    }

    private fun repositoryRoot(): Path =
        generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("app/src/main/kotlin")) }

    private companion object {
        const val removePackagePath = "com/wire/android/ui/authentication/devices/remove"
        val movedSourceNames = listOf(
            "RemoveDeviceViewModel.kt",
            "RemoveDeviceAuthenticationState.kt",
            "RemoveDeviceGateway.kt",
        )
        val forbiddenImportFragments = listOf(
            "com.wire.kalium",
            "com.wire.android.BuildConfig",
            "com.wire.android.datastore",
            "com.wire.android.ui.authentication.devices.model.Device",
            "dev.zacsweers.metro",
            "ClientId",
            "CoreFailure",
            "DeleteClientParam",
            "RegisterClientParam",
            "SelfClientsResult",
            "com.wire.android.navigation",
        )
    }
}
