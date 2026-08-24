/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */

package com.wire.android.ui.authentication

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuthenticationChromeOwnershipTest {

    @Test
    fun givenAuthenticationChromeSources_whenInspectingOwners_thenFeatureOwnsMovedSources() {
        val repositoryRoot = repositoryRoot()
        movedSources.forEach { source ->
            val appFile = repositoryRoot.resolve("app/src/main/kotlin/$source")
            val featureFile = repositoryRoot.resolve("features/authentication/src/main/kotlin/$source")

            assertFalse(Files.exists(appFile), "App still owns $source")
            assertTrue(Files.isRegularFile(featureFile), "Feature does not own $source")

            val content = Files.readString(featureFile)
            assertTrue(content.contains("package com.wire.android.ui.authentication"))
            assertFalse(content.contains("import com.wire.android.R"))
            assertFalse(content.contains("com.wire.kalium"))
            assertFalse(content.contains("com.wire.android.util"))
            assertFalse(content.contains("PreviewMultipleThemes"))
        }
    }

    @Test
    fun givenAuthenticationChromeResources_whenInspectingOwners_thenResourcesMovedExactly() {
        val repositoryRoot = repositoryRoot()
        val appResources = repositoryRoot.resolve("app/src/main/res")
        val featureResources = repositoryRoot.resolve("features/authentication/src/main/res")

        assertFalse(Files.exists(appResources.resolve("drawable/bg_waves.xml")))
        val featureDrawable = featureResources.resolve("drawable/bg_waves.xml")
        assertTrue(Files.isRegularFile(featureDrawable))
        assertEquals("532ace4c187951543e0ed494e9481e030b5676d61d8560134cb95b2304b2a57b", sha256(Files.readAllBytes(featureDrawable)))

        val appDefinitions = resourceDefinitions(appResources)
        val featureDefinitions = resourceDefinitions(featureResources)
        assertTrue(appDefinitions.isEmpty(), "App still owns remove-device strings: $appDefinitions")
        assertEquals(
            expectedQualifiersByResource,
            featureDefinitions
            .groupBy { definition -> resourceNames.single { name -> definition.contains("name=\"${name}\"") } }
            .mapValues { (_, definitions) -> definitions.map { it.substringBefore('|') }.toSet() }
        )
        assertEquals(39, featureDefinitions.size)
        assertEquals("55d5c5aac64fd9304ce257d778db9914f04b3c8303813177c9cb2d692c090b8b", sha256(featureDefinitions.joinToString("\n")))
    }

    private fun resourceDefinitions(resourceRoot: Path): List<String> =
        Files.walk(resourceRoot).use { paths ->
            paths.filter { path ->
                Files.isRegularFile(path) && path.parent.fileName.toString().startsWith("values")
            }.flatMap { path ->
                val qualifier = path.parent.fileName.toString()
                resourceRegex.findAll(Files.readString(path)).map { match ->
                    "$qualifier|${match.value.trim()}"
                }.toList().stream()
            }.sorted().toList()
        }

    private fun repositoryRoot(): Path =
        generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("app/src/main/kotlin")) }

    private fun sha256(value: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(value)
            .joinToString("") { byte -> "%02x".format(byte) }

    private fun sha256(value: String): String = sha256(value.toByteArray(StandardCharsets.UTF_8))

    private companion object {
        val movedSources = setOf(
            "com/wire/android/ui/authentication/login/WireAuthBackgroundComponent.kt",
            "com/wire/android/ui/authentication/devices/remove/RemoveDeviceTopBar.kt",
            "com/wire/android/ui/authentication/devices/register/RegisterDeviceVerificationCodeScreen.kt",
        )
        val resourceNames = setOf("remove_device_title", "remove_device_message", "remove_device_label")
        val resourceRegex = Regex(
            """<string\s+name="(${resourceNames.joinToString("|")})"[^>]*>.*?</string>""",
            RegexOption.DOT_MATCHES_ALL,
        )
        val expectedQualifiersByResource = mapOf(
            "remove_device_title" to setOf(
                "values", "values-de", "values-es", "values-fr", "values-hr", "values-hu",
                "values-it", "values-ja", "values-pl", "values-pt", "values-ru", "values-si", "values-sv",
            ),
            "remove_device_message" to setOf(
                "values", "values-de", "values-es", "values-fr", "values-hr", "values-hu",
                "values-it", "values-ja", "values-pl", "values-pt", "values-ru", "values-si",
            ),
            "remove_device_label" to setOf(
                "values", "values-de", "values-es", "values-et", "values-fr", "values-hr", "values-hu",
                "values-it", "values-ja", "values-pl", "values-pt", "values-ru", "values-si", "values-sv",
            ),
        )
    }
}
