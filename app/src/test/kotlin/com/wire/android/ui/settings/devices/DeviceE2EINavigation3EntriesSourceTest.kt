/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.settings.devices

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeviceE2EINavigation3EntriesSourceTest {

    @Test
    fun givenDeviceContribution_whenInspectingEntries_thenAllSixTypedRoutesAreRegistered() {
        val source = sourceFile(
            "ui/settings/devices/DeviceE2EINavigation3Entries.kt"
        ).readText()

        listOf(
            "wireEntry<RegisterDeviceRoute>",
            "wireEntry<RemoveDeviceRoute>",
            "wireEntry<E2EIEnrollmentRoute>",
            "wireEntry<SelfDevicesRoute>",
            "wireEntry<DeviceDetailsRoute>",
            "wireEntry<E2eiCertificateDetailsRoute>",
        ).forEach { registration ->
            assertTrue(source.contains(registration), "Missing $registration")
        }
        assertTrue(source.contains("resultTypes: List<WireNavigation3ResultType<*>> = emptyList()"))
    }

    @Test
    fun givenDeviceNavigation3Entries_whenInspectingSource_thenNoLegacyArgumentBridgeIsUsed() {
        val entries = sourceFile(
            "ui/settings/devices/DeviceE2EINavigation3Entries.kt"
        ).readText()

        listOf(
            "com.ramcosta.composedestinations",
            "ScreenDestination",
            "SavedStateHandle",
            "Bundle",
            "DEFAULT_ARGS_KEY",
            ".navArgs()",
        ).forEach { forbidden ->
            assertFalse(entries.contains(forbidden), "Entries must not reference $forbidden")
        }
        assertTrue(entries.contains("deviceDetailsViewModel(route.toViewModelArgs())"))
        assertTrue(entries.contains("e2eiCertificateDetailsViewModel(route.toViewModelArgs())"))
    }

    @Test
    fun givenDeviceRouteContracts_whenInspectingImports_thenTheyRemainKmpSourcePure() {
        listOf(
            "ui/authentication/devices/register/RegisterDeviceRoute.kt",
            "ui/authentication/devices/remove/RemoveDeviceRoute.kt",
            "ui/e2eiEnrollment/E2EIEnrollmentRoute.kt",
            "ui/settings/devices/DeviceManagementNavigation3.kt",
            "ui/settings/devices/e2ei/E2eiCertificateDetailsNavigation3.kt",
        ).forEach { relativePath ->
            val source = sourceFile(relativePath).readText()
            listOf("android.", "androidx.", "com.ramcosta.", "com.wire.kalium.").forEach { prefix ->
                assertFalse(
                    source.lineSequence()
                        .filter { it.startsWith("import ") }
                        .any { it.removePrefix("import ").startsWith(prefix) },
                    "$relativePath must not import $prefix",
                )
            }
        }
    }

    @Test
    fun givenDeviceRemovalCompletes_whenInspectingScreen_thenBackNavigationIsAOneShotEffect() {
        val source = sourceFile("ui/settings/devices/DeviceDetailsScreen.kt").readText()

        assertTrue(source.contains("LaunchedEffect(shouldNavigateBack)"))
        assertFalse(source.contains("viewModel.state.deviceRemoved -> onNavigateBack()"))
        assertFalse(source.contains("RemoveDeviceError.InitError -> onNavigateBack()"))
    }

    @Test
    fun givenRegisterDeviceCompletes_whenInspectingFlow_thenNavigationIsAppliedOnlyOnce() {
        val screen = sourceFile(
            "ui/authentication/devices/register/RegisterDeviceScreen.kt"
        ).readText()
        val entries = sourceFile(
            "ui/settings/devices/DeviceE2EINavigation3Entries.kt"
        ).readText()
        val router = sourceFile(
            "navigation/routes/auth/AuthenticationNavigation3Router.kt"
        ).readText()

        assertTrue(screen.contains("LaunchedEffect(flowState)"))
        assertTrue(entries.contains("route.registerDeviceTerminalEventId()"))
        assertTrue(router.contains("executeTerminalTransitionOnce(eventId, \"REGISTER_DEVICE_TERMINAL\")"))
        assertFalse(screen.contains("is RegisterDeviceFlowState.TooManyDevices -> onRemoveDeviceRequired()\n        else ->"))
    }

    private fun sourceFile(relativePath: String): File {
        val projectDir = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "app/src/main/kotlin").isDirectory }
        val appSource = File(projectDir, "app/src/main/kotlin/com/wire/android/$relativePath")
        val authenticationSource = File(
            projectDir,
            "features/authentication/src/main/kotlin/com/wire/android/$relativePath",
        )
        return listOf(appSource, authenticationSource).firstOrNull(File::isFile)
            ?: error("Missing source file $relativePath")
    }
}
