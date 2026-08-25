/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuthenticationCrowdinConfigurationTest {

    @Test
    fun givenLocalizedAuthenticationResources_thenEverySourceHasCrowdinConfiguration() {
        val root = repositoryRoot()
        val resourceRoot = root.resolve("features/authentication/src/main/res")
        val sourceDirectory = resourceRoot.resolve("values")
        val crowdinConfiguration = Files.readString(root.resolve("crowdin.yml"))
        val localizedSources = Files.list(sourceDirectory).use { sourceFiles ->
            sourceFiles.filter { source ->
                source.fileName.toString().endsWith(".xml") &&
                    localizedResourceDirectories(resourceRoot, source.fileName).isNotEmpty()
            }.map { it.fileName.toString() }.sorted().toList()
        }

        assertTrue(localizedSources.isNotEmpty())
        localizedSources.forEach { fileName ->
            val source = "/features/authentication/src/main/res/values/$fileName"
            val translation = "/features/authentication/src/main/res/values-%two_letters_code%/%original_file_name%"
            assertTrue(
                crowdinConfiguration.contains("\"source\": \"$source\",\n        \"translation\": \"$translation\""),
                "Missing Crowdin configuration for $fileName",
            )
        }
    }

    private fun localizedResourceDirectories(resourceRoot: Path, fileName: Path): List<Path> =
        Files.list(resourceRoot).use { directories ->
            directories.filter { directory ->
                Files.isDirectory(directory) &&
                    directory.fileName.toString().startsWith("values-") &&
                    Files.isRegularFile(directory.resolve(fileName))
            }.toList()
        }

    private fun repositoryRoot(): Path = generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) {
        it.parent
    }.first { Files.isDirectory(it.resolve("features/authentication")) }
}
