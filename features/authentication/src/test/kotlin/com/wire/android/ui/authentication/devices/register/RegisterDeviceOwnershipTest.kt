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

package com.wire.android.ui.authentication.devices.register

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RegisterDeviceOwnershipTest {

    @Test
    fun givenRegisterDeviceStateEngine_thenFeatureOwnsPreservedSourcesAndAppOwnsAdapters() {
        val root = repositoryRoot()
        movedSourceNames.forEach { sourceName ->
            val relativePath = "$registerPackagePath/$sourceName"
            assertFalse(Files.exists(root.resolve("app/src/main/kotlin/$relativePath")))
            assertTrue(Files.isRegularFile(root.resolve("features/authentication/src/main/kotlin/$relativePath")))
        }
        assertTrue(
            Files.isRegularFile(
                root.resolve("app/src/main/kotlin/$registerPackagePath/RegisterDeviceGatewayAdapter.kt")
            )
        )
    }

    @Test
    fun givenFeatureRegisterDeviceSources_thenNoHostOrKaliumTypesCrossTheBoundary() {
        val root = repositoryRoot()
        val featureRegisterRoot = root.resolve("features/authentication/src/main/kotlin/$registerPackagePath")
        val sources = Files.walk(featureRegisterRoot).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".kt") }
                .filter { it.fileName.toString() != "RegisterDeviceRoute.kt" }
                .map(Files::readString)
                .toList()
        }

        forbiddenSourceFragments.forEach { forbidden ->
            assertFalse(sources.any { it.contains(forbidden) }, "Forbidden feature dependency: $forbidden")
        }
        assertTrue(sources.any { it.contains("interface RegisterDeviceGateway") })
        assertTrue(sources.any { it.contains("RegisterDeviceGateway<SessionT>") })
        assertFalse(sources.any { it.contains("WireSessionId") })
        assertTrue(sources.any { it.contains("fun interface RegisterDeviceResendTimer") })
        assertTrue(sources.any { it.contains("const val RESEND_TIMER_DELAY_SECONDS = 300L") })
    }

    private fun repositoryRoot(): Path =
        generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("app/src/main/kotlin")) }

    private companion object {
        const val registerPackagePath = "com/wire/android/ui/authentication/devices/register"
        val movedSourceNames = listOf("RegisterDeviceViewModel.kt", "RegisterDeviceState.kt")
        val forbiddenSourceFragments = listOf(
            "com.wire.kalium",
            "com.wire.android.BuildConfig",
            "com.wire.android.datastore",
            "LoginEmailViewModel",
            "dev.zacsweers.metro",
            "CoreFailure",
            "ClientId",
            "UserId",
        )
    }
}
