/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 or any later version.
 */

package com.wire.android.navigation.routes.auth

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuthenticationNavigation3RouterSourceTest {

    @Test
    fun givenAuthenticationEntryProviders_whenInspectingMutations_thenOnlyRouterChangesTheStack() {
        authenticationProviderSources.forEach { source ->
            val completeContent = projectSource(source).readText()
            val content = if (source.endsWith("DeviceE2EINavigation3Entries.kt")) {
                completeContent.substringBefore("private fun SelfDevicesNavigation3Entry")
            } else {
                completeContent
            }

            listOf(
                "runtime.navigator.navigate(",
                "runtime.navigator.goBack(",
                "runtime.navigator.replaceBackStack(",
            ).forEach { forbidden ->
                assertFalse(
                    forbidden in content,
                    "$source must dispatch authentication transitions through AuthenticationNavigation3Router",
                )
            }
        }
    }

    @Test
    fun givenExecutableTopology_whenInspectingTransitions_thenEveryNamedEdgeIsUsed() {
        val router = projectSource(routerSource).readText()
        val productionSources = buildString {
            append(router)
            authenticationProviderSources.forEach { append(projectSource(it).readText()) }
            append(projectSource("app/src/main/kotlin/com/wire/android/ui/WireActivityNavigation3Host.kt").readText())
            append(projectSource("app/src/main/kotlin/com/wire/android/ui/WireActivityNavigation3Effects.kt").readText())
            append(projectSource("app/src/main/kotlin/com/wire/android/ui/WireActivityNavigation3SessionEffects.kt").readText())
            append(projectSource("app/src/main/kotlin/com/wire/android/navigation/runtime/WireNavigation3ProductionActions.kt").readText())
        }
        val transitionNames = Regex("""^\s{4}([A-Z][A-Z0-9_]+),?$""", RegexOption.MULTILINE)
            .findAll(
                router.substringAfter("internal enum class AuthenticationNavigationTransition")
                    .substringBefore("\n}"),
            )
            .map { it.groupValues[1] }
            .toList()

        assertTrue(transitionNames.isNotEmpty())
        transitionNames.forEach { transition ->
            assertTrue(
                "AuthenticationNavigationTransition.$transition" in productionSources,
                "Authentication topology edge $transition is declared but not executable",
            )
        }
    }

    private fun projectSource(relativePath: String): File =
        generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .map { File(it, relativePath) }
            .first(File::isFile)

    private companion object {
        const val routerSource =
            "app/src/main/kotlin/com/wire/android/navigation/routes/auth/AuthenticationNavigation3Router.kt"
        val authenticationProviderSources = listOf(
            "app/src/main/kotlin/com/wire/android/navigation/routes/auth/AuthenticationNavigation3Entries.kt",
            "app/src/main/kotlin/com/wire/android/navigation/routes/auth/CreateAccountNavigation3Entries.kt",
            "app/src/main/kotlin/com/wire/android/ui/settings/devices/DeviceE2EINavigation3Entries.kt",
        )
    }
}
