/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.initialsync

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InitialSyncResourceOwnershipTest {
    @Test
    fun givenInitialSyncResources_whenInspectingAllQualifiers_thenFeatureOwnsExactContent() {
        val root = repositoryRoot()
        val featureFiles = Files.walk(root.resolve("features/authentication/src/main/res")).use { paths ->
            paths.filter { it.fileName.toString() == "initial_sync.xml" }.toList()
        }

        assertEquals(expectedDigests.keys, featureFiles.map { qualifier(it) }.toSet())
        featureFiles.forEach { file ->
            assertEquals(expectedDigests[qualifier(file)], sha256(file))
            assertEquals(setOf("migration_title", "migration_message"), stringNames(file).toSet())
        }

        val appNames = Files.walk(root.resolve("app/src/main/res")).use { paths ->
            paths.filter { it.fileName.toString().endsWith(".xml") }
                .flatMap { stringNames(it).stream() }
                .toList()
                .toSet()
        }
        assertFalse(appNames.contains("migration_title"))
        assertFalse(appNames.contains("migration_message"))

        val appDrawable = root.resolve("app/src/main/res/drawable/ic_migration.xml")
        val featureDrawable = root.resolve("features/authentication/src/main/res/drawable/ic_migration.xml")
        assertFalse(Files.exists(appDrawable))
        assertTrue(Files.isRegularFile(featureDrawable))
        assertEquals(migrationDrawableDigest, sha256(featureDrawable))
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
            "values" to "51c48c43510aa76535120fb67ed9687724169c1be8b1a8985d222702a63bac50",
            "values-de" to "8f651a6c6a4177665b696206bb94e2d50a943c0b3d2d26e8805cd75ab8811426",
            "values-es" to "adcc629007aef77ba1a8c8e52d1e8d3c0b5298f850338fa0c89c4f1a2a23f2a6",
            "values-fr" to "b617ef683c293a1d94ef6c5aa32ea1c227cb12b124b76cc4bb12440318cbfb28",
            "values-hr" to "589ea2e7d90d236e3926ebad73bb534c40c1083ad62051a008ea4c2eec0aa0f1",
            "values-hu" to "9a55fb710382af91b72cd3f2c45365b6d6d9a623e8799edbfd7b7c4670078877",
            "values-it" to "9f282b711c975d8c0677e7fe1667eaf54d45b3af94f0e00c4096e692d2c2ad8d",
            "values-ja" to "1ddf6e374926334a2cfecf7182696e987dcd78d51ca50cdf35a7cf5e6a2b35ff",
            "values-pl" to "57e8723fca0008484ed8d18ee4a18337a01aa36756098d80458ed89c4f10fc91",
            "values-pt" to "1eaa79c1e45171b22af20773ad910359640d5111a31c798480b1c8718ee22b32",
            "values-ru" to "0e49d18c5b7e4c4ca5a20bd73f3709a3c9e4e91b60afd4045e33dc7926d82ea6",
            "values-si" to "c38b0c8283d7cf4a5578a47dcf868f1e0e1422e3bd6219a9105dfaad27831fbd",
            "values-sv" to "c2e7d311ecf58042a2c5f89968048ce2b4a2979ed47e00b13913ef4a4052d87d",
            "values-tr" to "bd0902dc7d28b9f35f0ad172d019fc7fd914a6d9638f42b3100dc2322d39d2a6",
        )

        const val migrationDrawableDigest = "35e3d0931a0430eabf76d16c210508557d24438ae221f0d7a6437b92f5a38d59"
    }
}
