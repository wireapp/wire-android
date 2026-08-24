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
package com.wire.android.ui.authentication.create.overview

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreateAccountOverviewOwnershipTest {

    @Test
    fun givenOverviewStateEngine_thenFeatureOwnsViewModelAndAppOwnsHostFactory() {
        val root = repositoryRoot()
        assertFalse(Files.exists(root.resolve("app/src/main/kotlin/$packagePath/CreateAccountOverviewViewModel.kt")))
        assertTrue(
            Files.isRegularFile(
                root.resolve("features/authentication/src/main/kotlin/$packagePath/CreateAccountOverviewViewModel.kt")
            )
        )
        assertTrue(
            Files.isRegularFile(
                root.resolve("app/src/main/kotlin/$packagePath/CreateAccountOverviewViewModelHostFactory.kt")
            )
        )
    }

    @Test
    fun givenOverviewPresentation_thenFeatureOwnsParamsAndRendererWhileAppOwnsHostAdaptation() {
        val root = repositoryRoot()
        val appPackage = root.resolve("app/src/main/kotlin/$packagePath")
        val featurePackage = root.resolve("features/authentication/src/main/kotlin/$packagePath")
        val params = Files.readString(featurePackage.resolve("CreateAccountOverviewParams.kt"))
        val content = Files.readString(featurePackage.resolve("CreateAccountOverviewContent.kt"))
        val appScreen = Files.readString(appPackage.resolve("CreatePersonalAccountOverviewScreen.kt"))

        assertFalse(Files.exists(appPackage.resolve("CreateAccountOverviewParams.kt")))
        assertTrue(Files.isRegularFile(featurePackage.resolve("CreateAccountOverviewParams.kt")))
        assertTrue(params.contains("package com.wire.android.ui.authentication.create.overview"))
        assertTrue(params.contains("data class CreateAccountOverviewParams("))
        assertTrue(params.contains("@DrawableRes val contentIconResId: Int = 0"))

        assertTrue(content.contains("fun CreateAccountOverviewContent("))
        assertTrue(content.contains("continueText: String"))
        assertTrue(content.contains("@StringRes backContentDescription: Int"))
        assertTrue(content.contains("modifier: Modifier = Modifier"))
        assertTrue(content.contains("onLearnMorePressed: (String) -> Unit"))
        assertTrue(content.contains("subtitleContent: @Composable ColumnScope.() -> Unit = {}"))
        assertTrue(content.contains("if (overviewParams.contentTitle.isNotEmpty())"))
        assertTrue(content.contains("onLearnMorePressed(overviewParams.learnMoreUrl)"))
        assertTrue(content.contains("contentScale = ContentScale.Inside"))
        assertTrue(content.contains("modifier = modifier"))
        assertTrue(content.contains(".clearAndSetSemantics {}"))
        assertFalse(content.contains("com.wire.android.R"))
        assertFalse(content.contains("ServerConfig"))
        assertFalse(content.contains("CustomTabsHelper"))
        assertFalse(content.contains("CreateAccountFlowType"))
        assertFalse(content.contains("CreateAccountNavArgs"))

        assertTrue(appScreen.contains("CreateAccountOverviewContent("))
        assertTrue(appScreen.contains("learnMoreUrl = viewModel.learnMoreUrl()"))
        assertTrue(appScreen.contains("customServerConfig = viewModel.customServerConfig"))
        assertTrue(appScreen.contains("CustomTabsHelper.launchUrl(context, url)"))
        assertTrue(appScreen.contains("if (viewModel.serverConfig.isOnPremises)"))
        assertTrue(appScreen.contains("ServerTitle("))
        assertFalse(appScreen.contains("WireScaffold("))
        assertFalse(appScreen.contains("OverviewTexts("))
    }

    @Test
    fun givenFeatureOverviewSources_thenHostTypesDoNotCrossBoundary() {
        val root = repositoryRoot()
        val sourceRoots = listOf(
            root.resolve("features/authentication/src/main/kotlin/$packagePath"),
            root.resolve("features/authentication/src/test/kotlin/$packagePath"),
        )
        val imports = sourceRoots.flatMap { sourceRoot ->
            Files.walk(sourceRoot).use { paths ->
                paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".kt") }
                    .flatMap { Files.lines(it) }
                    .filter { it.startsWith("import ") }
                    .toList()
            }
        }
        forbiddenImports.forEach { forbidden ->
            assertFalse(imports.any { it.contains(forbidden) }, "Forbidden feature dependency: $forbidden")
        }

        val source = Files.readString(
            root.resolve("features/authentication/src/main/kotlin/$packagePath/CreateAccountOverviewViewModel.kt")
        )
        assertTrue(source.contains("class CreateAccountOverviewViewModel<LinksT>"))
        assertTrue(source.contains("val customServerConfig: LinksT?"))
        assertTrue(source.contains("private val pricingUrl: (LinksT) -> String"))
    }

    @Test
    fun givenOverviewResources_whenInspectingOwners_thenStringsAndDrawablesMovedExactly() {
        val root = repositoryRoot()
        val appResources = root.resolve("app/src/main/res")
        val featureResources = root.resolve("features/authentication/src/main/res")
        val appDefinitions = resourceDefinitions(appResources)
        val featureDefinitions = resourceDefinitions(featureResources)

        assertTrue(appDefinitions.isEmpty(), "App still owns overview strings: $appDefinitions")
        assertEquals(45, featureDefinitions.size)
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
            assertEquals(listOf(featureDrawable), drawableFiles(featureResources, name))
        }
    }

    private fun resourceDefinitions(resourceRoot: Path): List<String> =
        Files.walk(resourceRoot).use { paths ->
            paths
                .filter { path ->
                    Files.isRegularFile(path) &&
                        path.fileName.toString().endsWith(".xml") &&
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

    private fun sha256(value: String): String = sha256(value.toByteArray(StandardCharsets.UTF_8))

    private fun sha256(value: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value)
            .joinToString("") { byte -> "%02x".format(byte) }

    private fun repositoryRoot(): Path =
        generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("app/src/main/kotlin")) }

    private companion object {
        const val packagePath = "com/wire/android/ui/authentication/create/overview"
        const val expectedResourceFingerprint = "923e9ecca15438f03942b14028a2055fbda1c8a1953281b043964c8271c2d05e"
        val forbiddenImports = listOf(
            "com.wire.kalium",
            "com.wire.android.BuildConfig",
            "CreateAccountOverviewNavArgs",
            "ServerConfig",
            "Parcelable",
            "dev.zacsweers.metro",
        )
        val resourceNames = setOf(
            "create_personal_account_text",
            "create_team_content_title",
            "create_team_text",
            "create_team_learn_more",
        )
        val resourceRegex = Regex(
            """<string\s+name="(${resourceNames.joinToString("|")})"[^>]*>.*?</string>"""
        )
        val expectedQualifiersByResource = mapOf(
            "create_personal_account_text" to setOf(
                "values", "values-de", "values-es", "values-fr", "values-hr", "values-hu",
                "values-it", "values-ja", "values-pl", "values-pt", "values-ru", "values-si",
            ),
            "create_team_content_title" to setOf(
                "values", "values-de", "values-es", "values-et", "values-fr", "values-hu",
                "values-it", "values-pl", "values-pt", "values-ru", "values-si",
            ),
            "create_team_text" to setOf(
                "values", "values-de", "values-es", "values-fr", "values-hr", "values-hu",
                "values-it", "values-pl", "values-pt", "values-ru", "values-si",
            ),
            "create_team_learn_more" to setOf(
                "values", "values-de", "values-es", "values-fr", "values-hr", "values-hu",
                "values-it", "values-pl", "values-pt", "values-ru", "values-si",
            ),
        )
        val drawableFingerprints = mapOf(
            "ic_create_personal_account" to
                "6c5d3f9810ea939e3f3d4080928e0410dadc41ad966761f89ba68db44bae2512",
            "ic_create_team" to
                "4cb306fc8da3ab2543fcc3b21bd2088c63368f0e92990eae950efbd3672b10f7",
        )
    }
}
