/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.userprofile

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UserProfileNavigation3SourcePurityTest {

    @Test
    fun givenUserProfileRouteContracts_whenInspectingImports_thenTheyAreKmpSourcePure() {
        contractFiles.forEach { relativePath ->
            val source = sourceFile(relativePath).readText()
            val imports = source
                .lineSequence()
                .filter { it.startsWith("import ") }
                .map { it.removePrefix("import ").substringBefore(" as ") }
                .toList()
            forbiddenImportPrefixes.forEach { forbidden ->
                assertFalse(
                    imports.any { it.startsWith(forbidden) },
                    "$relativePath must not import $forbidden",
                )
            }
            val sourceWithoutComments = source
                .replace(Regex("/\\*[\\s\\S]*?\\*/"), "")
                .replace(Regex("//.*"), "")
            forbiddenContractTypes.forEach { forbidden ->
                assertFalse(
                    Regex("\\b${Regex.escape(forbidden)}\\b").containsMatchIn(sourceWithoutComments),
                    "$relativePath must not reference $forbidden",
                )
            }
        }
    }

    @Test
    fun givenUserProfileContribution_whenInspectingEntries_thenAllFiveRoutesAreRegistered() {
        val source = sourceFile("UserProfileNavigation3Entries.kt").readText()

        listOf(
            "wireEntry<AvatarPickerRoute>",
            "wireEntry<SelfQrCodeRoute>",
            "wireEntry<SelfUserProfileRoute>",
            "wireEntry<OtherUserProfileRoute>",
            "wireEntry<ServiceDetailsRoute>",
        ).forEach { registration ->
            assertTrue(source.contains(registration), "Missing $registration")
        }
        assertTrue(source.contains("AvatarPickerNavigation3ResultType"))
        assertTrue(source.contains("ConnectionRequestIgnoredNavigation3ResultType"))
        assertFalse(source.contains("ScreenDestination"))
        assertFalse(source.contains("com.ramcosta.composedestinations"))
    }

    @Test
    fun givenArgumentBackedViewModels_whenInspectingSources_thenTheyUseTypedAssistedArguments() {
        listOf(
            "qr/SelfQRCodeViewModel.kt" to "SelfQrCodeViewModelArgs",
            "other/OtherUserProfileScreenViewModel.kt" to "OtherUserProfileViewModelArgs",
            "service/ServiceDetailsViewModel.kt" to "ServiceDetailsViewModelArgs",
        ).forEach { (relativePath, argumentType) ->
            val source = sourceFile(relativePath).readText()
            assertTrue(source.contains("navigationArgs: $argumentType"))
            assertFalse(source.contains("SavedStateHandle"))
            assertFalse(source.contains("generated.app.navArgs"))
        }

        val entries = sourceFile("UserProfileNavigation3Entries.kt").readText()
        assertTrue(entries.contains("toSelfQrCodeViewModelArgs()"))
        assertTrue(entries.contains("toOtherUserProfileViewModelArgs()"))
        assertTrue(entries.contains("toServiceDetailsViewModelArgs()"))
    }

    private fun sourceFile(relativePath: String): File {
        val projectDir = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "app/src/main/kotlin").isDirectory }
        return File(
            projectDir,
            "app/src/main/kotlin/com/wire/android/ui/userprofile/$relativePath",
        ).also {
            assertTrue(it.isFile, "Missing source file $relativePath")
        }
    }

    private companion object {
        val contractFiles = listOf(
            "UserProfileQualifiedId.kt",
            "avatarpicker/AvatarPickerNavigation3.kt",
            "qr/SelfQrCodeNavigation3.kt",
            "self/SelfUserProfileRoute.kt",
            "other/OtherUserProfileNavigation3.kt",
            "service/ServiceDetailsNavigation3.kt",
        )
        val forbiddenImportPrefixes = listOf(
            "android.",
            "androidx.",
            "com.ramcosta.",
            "com.wire.kalium.",
        )
        val forbiddenContractTypes = listOf(
            "Parcelable",
            "Parcelize",
            "NavArgs",
            "Destination",
        )
    }
}
