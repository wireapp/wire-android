/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.legacyregistration

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LegacyRegistrationResourceOwnershipTest {
    @Test
    fun givenLegacyRegistrationTranslations_whenInspectingAllQualifiers_thenValuesAndOwnershipArePreserved() {
        val root = repositoryRoot()
        val featureFiles = resourceFiles(root.resolve("features/authentication/src/main/res"))
        val appResourceNames = xmlFiles(root.resolve("app/src/main/res"))
            .flatMap(::stringNames)
            .toSet()

        assertEquals(expectedDigests.keys, featureFiles.map { it.fileName.toString() to qualifier(it) }.toSet())
        featureFiles.forEach { file ->
            val resourceNames = stringNames(file)

            assertEquals(expectedDigests[file.fileName.toString() to qualifier(file)], sha256(file))
            assertTrue(resourceNames.isNotEmpty(), "$file has no legacy-registration resources")
            assertTrue(resourceNames.none(String::isBlank), "$file contains a blank resource name")
            assertFalse(
                resourceNames.any(appResourceNames::contains),
                "$file retains a duplicate app resource: ${resourceNames.intersect(appResourceNames)}",
            )
        }
    }

    private fun resourceFiles(root: Path): List<Path> = Files.walk(root).use { paths ->
        paths.filter { path -> path.fileName.toString() == "legacy_registration.xml" }
            .toList()
    }

    private fun xmlFiles(root: Path): List<Path> = Files.walk(root).use { paths ->
        paths.filter { path -> path.fileName.toString().endsWith(".xml") }
            .toList()
    }

    private fun stringNames(path: Path): List<String> = stringPattern
        .findAll(Files.readString(path))
        .map { it.groupValues[1] }
        .toList()

    private fun qualifier(path: Path): String = path.parent.fileName.toString()

    private fun sha256(path: Path): String = MessageDigest.getInstance("SHA-256")
        .digest(Files.readAllBytes(path))
        .joinToString("") { byte -> "%02x".format(byte) }

    private fun repositoryRoot(): Path =
        generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("app/src/main/kotlin")) }

    private companion object {
        val stringPattern = Regex("""<string\s+name="([^"]+)"""")

        val expectedDigests = mapOf(
            ("legacy_registration.xml" to "values") to "985c73de2a498196f6bedf8014efe0b384688e7e7e37269a2a9ccf0442e1daa1",
            ("legacy_registration.xml" to "values-de") to "18d27438c7065ab5a276bd4c665ab8056b4132d0ed384b187d03720073a83166",
            ("legacy_registration.xml" to "values-es") to "441b3bb3ad268a038069f544e7c12126292d11941035fb06bb2516741b529523",
            ("legacy_registration.xml" to "values-et") to "6892494cc7e1a071cf51b0d00e2ac9001df7f3e4588d2de9de730f7dcb2800d8",
            ("legacy_registration.xml" to "values-fr") to "0b4a57f961a2b04de156890aa337ccca035e409a81e6c2ca6be1223b1c44da5c",
            ("legacy_registration.xml" to "values-hr") to "b76e00ce776501b7e5fe68f36bd2f1e375f5deb0c20fdc9ac6a3f98429b03bbb",
            ("legacy_registration.xml" to "values-hu") to "0e3caacefcc06b7bc56c43518acd0b36b761cc36ca226d66cb4fd4a42b62bb6f",
            ("legacy_registration.xml" to "values-it") to "c9c57b8e064cf367a6b257264ff174f031a582afcf188866d528993915c9e193",
            ("legacy_registration.xml" to "values-ja") to "56caa81d486ad92cc65678ae26ecc429480dc0d2ce2a0b0a510fd110cfa7795a",
            ("legacy_registration.xml" to "values-pl") to "34a822ac32e36b18e1665441221609ae86f056ff80a8c8d7736ca52d2f0b3258",
            ("legacy_registration.xml" to "values-pt") to "8af82be17dc913a7e77b54e9b5807e3944d6125c1a059f0dc95776b131a08918",
            ("legacy_registration.xml" to "values-ru") to "c6b3a6fd31a2b52dd58918c0962a36665f464b665d6a0b9f5f6a8131ea07c2c3",
            ("legacy_registration.xml" to "values-si") to "b8338a7b446869666514af758ae4cf3f184f5d505f000947fdb8cacd34af55fe",
            ("legacy_registration.xml" to "values-sv") to "0024d9037a22ec83aee1cdd5dc6a12811b8e1dbf7df5d4647f948fddd1e16fd5",
            ("legacy_registration.xml" to "values-tr") to "2aab26b0aafe09300bd35fd3d3f0fec2a1060280df950fac5fa6f4a38e5a7a67",
            ("legacy_registration.xml" to "values-uk") to "46d060dbc54bd69901b08d42716c3080db0c378246bc1e82d797668286dc92e3",
        )
    }
}
