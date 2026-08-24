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

package com.wire.android.ui.authentication.create.summary

import com.wire.android.feature.authentication.R
import com.wire.android.navigation.routes.auth.CreateAccountRouteFlowType
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreateAccountSummaryOwnershipTest {

    @Test
    fun givenPersonalAndTeamFlows_whenMappingSummaryResources_thenFeatureResourcesAreSelected() {
        assertEquals(
            CreateAccountSummaryResources(
                title = R.string.create_personal_account_summary_title,
                text = R.string.create_personal_account_summary_text,
                icon = R.drawable.ic_create_personal_account_success,
            ),
            CreateAccountRouteFlowType.PERSONAL.summaryResources(),
        )
        assertEquals(
            CreateAccountSummaryResources(
                title = R.string.create_team_summary_title,
                text = R.string.create_team_summary_text,
                icon = R.drawable.ic_create_team_success,
            ),
            CreateAccountRouteFlowType.TEAM.summaryResources(),
        )
    }

    @Test
    fun givenCreateAccountSummarySources_whenInspectingOwners_thenOnlyStatelessFeatureUiRemains() {
        val repositoryRoot = repositoryRoot()
        val appPackage = repositoryRoot.resolve("app/src/main/kotlin/$packagePath")
        val featureScreen = repositoryRoot.resolve(
            "features/authentication/src/main/kotlin/$packagePath/CreateAccountSummaryScreen.kt"
        )

        assertFalse(Files.exists(appPackage.resolve("CreateAccountSummaryScreen.kt")))
        assertFalse(Files.exists(appPackage.resolve("CreateAccountSummaryViewModel.kt")))
        assertFalse(Files.exists(appPackage.resolve("CreateAccountSummaryViewState.kt")))
        assertTrue(Files.isRegularFile(appPackage.resolve("CreateAccountSummaryNavArgs.kt")))
        assertTrue(Files.isRegularFile(featureScreen))

        val source = Files.readString(featureScreen)
        assertTrue(source.contains("package $packageName"))
        assertTrue(source.contains("fun CreateAccountSummaryRouteScreen("))
        assertTrue(source.contains("type: CreateAccountRouteFlowType"))
        assertTrue(source.contains("onContinue: () -> Unit"))
        assertTrue(source.contains("import com.wire.android.feature.authentication.R"))
        assertTrue(source.contains("import com.wire.android.ui.common.preview.MultipleThemePreviews"))
        assertFalse(source.contains("CreateAccountSummaryViewModel"))
        assertFalse(source.contains("CreateAccountSummaryViewState"))
        assertFalse(source.contains("com.wire.android.R"))
        assertFalse(source.contains("com.wire.kalium"))
        assertFalse(source.contains("com.wire.android.di"))
    }

    @Test
    fun givenSummaryResources_whenInspectingOwners_thenStringsAndDrawablesMovedExactly() {
        val repositoryRoot = repositoryRoot()
        val appResources = repositoryRoot.resolve("app/src/main/res")
        val featureResources = repositoryRoot.resolve("features/authentication/src/main/res")
        val appDefinitions = resourceDefinitions(appResources)
        val featureDefinitions = resourceDefinitions(featureResources)

        assertTrue(appDefinitions.isEmpty(), "App still owns summary strings: $appDefinitions")
        assertEquals(65, featureDefinitions.size)
        assertEquals(
            expectedQualifiersByResource,
            featureDefinitions
                .groupBy { definition ->
                    resourceNames.single { name -> definition.contains("name=\"$name\"") }
                }
                .mapValues { (_, definitions) -> definitions.map { it.substringBefore('|') }.toSet() },
        )
        assertEquals(expectedResourceFingerprint, sha256(featureDefinitions.joinToString("\n")))

        drawableFingerprints.forEach { (name, expectedFingerprint) ->
            assertTrue(drawableFiles(appResources, name).isEmpty(), "App still owns $name")
            val featureDrawable = featureResources.resolve("drawable/$name.xml")
            assertTrue(Files.isRegularFile(featureDrawable))
            assertEquals(expectedFingerprint, sha256(Files.readAllBytes(featureDrawable)))
            assertEquals(
                listOf(featureDrawable),
                drawableFiles(featureResources, name),
                "Unexpected qualifier variant for $name",
            )
        }
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
                    val qualifier = path.parent.fileName.toString()
                    resourceRegex.findAll(Files.readString(path)).map { match ->
                        "$qualifier|${match.value.trim()}"
                    }.toList().stream()
                }
                .sorted()
                .toList()
        }

    private fun drawableFiles(resourceRoot: Path, name: String): List<Path> =
        Files.walk(resourceRoot).use { paths ->
            paths
                .filter { path ->
                    Files.isRegularFile(path) &&
                        path.fileName.toString() == "$name.xml" &&
                        path.parent.fileName.toString().startsWith("drawable")
                }
                .sorted()
                .toList()
        }

    private fun repositoryRoot(): Path =
        generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("app/src/main/kotlin")) }

    private fun sha256(value: String): String = sha256(value.toByteArray(StandardCharsets.UTF_8))

    private fun sha256(value: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value)
            .joinToString("") { byte -> "%02x".format(byte) }

    private companion object {
        const val packageName = "com.wire.android.ui.authentication.create.summary"
        const val packagePath = "com/wire/android/ui/authentication/create/summary"
        const val expectedResourceFingerprint = "860ddcaf56de7066d4e4ec9bc9249f54adb0787c5e59c41ec99ed1694a43019f"

        val resourceNames = setOf(
            "create_personal_account_summary_title",
            "create_personal_account_summary_text",
            "create_team_summary_title",
            "create_team_summary_text",
            "label_get_started",
        )
        val resourceRegex = Regex(
            """<string\s+name="(${resourceNames.joinToString("|")})"[^>]*>.*?</string>"""
        )
        val expectedQualifiersByResource = mapOf(
            "create_personal_account_summary_title" to setOf(
                "values", "values-de", "values-es", "values-fr", "values-hr", "values-hu",
                "values-it", "values-ja", "values-pl", "values-pt", "values-ru", "values-si",
            ),
            "create_personal_account_summary_text" to setOf(
                "values", "values-de", "values-es", "values-fr", "values-hr", "values-hu",
                "values-it", "values-pl", "values-pt", "values-ru", "values-si",
            ),
            "create_team_summary_title" to setOf(
                "values", "values-de", "values-es", "values-et", "values-fr", "values-hr",
                "values-hu", "values-it", "values-pl", "values-pt", "values-ru", "values-si",
            ),
            "create_team_summary_text" to setOf(
                "values", "values-de", "values-es", "values-fr", "values-hr", "values-hu",
                "values-it", "values-pl", "values-pt", "values-ru", "values-si",
            ),
            "label_get_started" to setOf(
                "values", "values-ar", "values-cs", "values-de", "values-es", "values-et",
                "values-fr", "values-hr", "values-hu", "values-it", "values-ja", "values-lt",
                "values-pl", "values-pt", "values-ru", "values-si", "values-sv", "values-tr",
                "values-uk",
            ),
        )
        val drawableFingerprints = mapOf(
            "ic_create_personal_account_success" to
                "91a0a6c1fedf4604d1aa65a6f53cf4a6a800e733fe4c564aa465f055ca284fab",
            "ic_create_team_success" to
                "72257d7eedf7b420af5e1194b146f6dc1e0414d48824742f1dfb1e31de6cab9c",
        )
    }
}
