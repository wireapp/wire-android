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

package com.wire.android.ui.authentication.verificationcode

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VerificationCodePrimitiveOwnershipTest {

    @Test
    fun givenVerificationCodePrimitives_whenInspectingSources_thenFeatureOwnsPreservedFqnsAndNeutralImports() {
        val repositoryRoot = repositoryRoot()
        sourceNames.forEach { sourceName ->
            val legacySource = repositoryRoot.resolve("app/src/main/kotlin/$packagePath/$sourceName")
            val featureSource = repositoryRoot.resolve(
                "features/authentication/src/main/kotlin/$packagePath/$sourceName"
            )

            assertFalse(Files.exists(legacySource), "Legacy source still exists: $legacySource")
            assertTrue(Files.isRegularFile(featureSource), "Missing feature source: $featureSource")

            val source = Files.readString(featureSource)
            assertTrue(source.contains("package $packageName"))
            assertTrue(source.contains("import com.wire.android.feature.authentication.R"))
            assertTrue(source.contains("import com.wire.android.ui.common.preview.MultipleThemePreviews"))
            assertFalse(source.contains("import com.wire.android.R"))
            assertFalse(source.contains("com.wire.android.util.ui.PreviewMultipleThemes"))
        }
    }

    @Test
    fun givenVerificationCodeResources_whenInspectingOwners_thenDefinitionsMovedExactlyOnceWithBaselineFingerprint() {
        val repositoryRoot = repositoryRoot()
        val appDefinitions = resourceDefinitions(repositoryRoot.resolve("app/src/main/res"))
        val featureDefinitions = resourceDefinitions(
            repositoryRoot.resolve("features/authentication/src/main/res")
        )

        assertTrue(appDefinitions.isEmpty(), "App still owns definitions: $appDefinitions")
        assertEquals(47, featureDefinitions.size)
        assertEquals(
            expectedQualifiersByResource,
            featureDefinitions
                .groupBy { definition ->
                    resourceNames.single { resourceName -> definition.contains("name=\"$resourceName\"") }
                }
                .mapValues { (_, definitions) -> definitions.map { it.substringBefore('|') }.toSet() }
        )
        assertEquals(expectedResourceFingerprint, sha256(featureDefinitions.joinToString("\n") + "\n"))
    }

    private fun resourceDefinitions(resourceRoot: Path): List<String> =
        Files.walk(resourceRoot).use { paths ->
            paths
                .filter { path ->
                    Files.isRegularFile(path) &&
                        path.fileName.toString() == "strings.xml" &&
                        path.parent.fileName.toString().startsWith("values")
                }
                .flatMap { path ->
                    Files.readAllLines(path).stream()
                        .filter { line -> resourceNames.any { name -> line.contains("<string name=\"$name\"") } }
                        .map { line -> "${path.parent.fileName}|$line" }
                }
                .sorted()
                .toList()
        }

    private fun repositoryRoot(): Path =
        generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("app/src/main/kotlin")) }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }

    private companion object {
        const val packageName = "com.wire.android.ui.authentication.verificationcode"
        const val packagePath = "com/wire/android/ui/authentication/verificationcode"
        const val expectedResourceFingerprint = "a0037151b3a0eb5819351ef87c1ff6998028a83e2f1123aa38ac31babdc60e69"

        val sourceNames = listOf(
            "ResendCodeText.kt",
            "VerificationCode.kt",
            "VerificationCodeScreenContent.kt",
        )
        val resourceNames = setOf(
            "create_account_code_resend",
            "second_factor_code_error",
            "second_factor_authentication_title",
            "second_factor_authentication_instructions_label",
        )
        val expectedQualifiersByResource = mapOf(
            "create_account_code_resend" to setOf(
                "values", "values-de", "values-es", "values-fr", "values-hr", "values-hu", "values-it",
                "values-pl", "values-pt", "values-ru", "values-si", "values-sv",
            ),
            "second_factor_code_error" to setOf(
                "values", "values-de", "values-es", "values-fr", "values-hr", "values-hu", "values-it",
                "values-ja", "values-pl", "values-pt", "values-ru", "values-si", "values-sv",
            ),
            "second_factor_authentication_title" to setOf(
                "values", "values-de", "values-es", "values-et", "values-fr", "values-hr", "values-hu",
                "values-it", "values-ja", "values-pl", "values-pt", "values-ru", "values-si", "values-sv",
            ),
            "second_factor_authentication_instructions_label" to setOf(
                "values", "values-de", "values-fr", "values-hu", "values-ja", "values-pt", "values-ru",
                "values-si",
            ),
        )
    }
}
