/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.create.code

import java.nio.file.Files
import java.nio.file.Path

internal object CreateAccountCodeOwnershipFixtures {
    const val packagePath = "com/wire/android/ui/authentication/create/code"

    val root: Path = generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
        .first { Files.isDirectory(it.resolve("app/src/main/kotlin")) }

    val sourceFiles = listOf(
        "CreateAccountCodeViewModel.kt",
        "CreateAccountCodeViewState.kt",
        "CreateAccountCodeGateway.kt",
        "CreateAccountCodeContent.kt",
    )

    val forbidden = listOf(
        "com.wire.kalium",
        "com.wire.android.BuildConfig",
        "CreateAccountNavArgs",
        "CreateAccountFlowType",
        "com.wire.kalium.logic.configuration.server.ServerConfig",
        "com.wire.kalium.logic.CoreLogic",
        "com.wire.kalium.logic.feature.register.RegisterParam",
        "com.wire.kalium.logic.data.session.StoreSessionParam",
        "com.wire.kalium.logic.feature.client.RegisterClientParam",
        "dev.zacsweers.metro",
        "com.wire.android.R",
        "com.wire.android.ui.registration.code.CreateAccountCodeResult",
    )

    val forbiddenImports = listOf(
        "com.wire.android.R",
        "CreateAccountFlowType",
        "CreateAccountNavArgs",
        "ServerConfig",
        "CoreFailure",
        "com.wire.kalium",
        "dev.zacsweers.metro",
        "DialogErrorStrings",
        "CustomTabs",
    )

    fun appSource(file: String): Path = root.resolve("app/src/main/kotlin/$packagePath/$file")

    fun featureSource(file: String): Path = root.resolve("features/authentication/src/main/kotlin/$packagePath/$file")

    fun featureCodeSource(): String = Files.walk(root.resolve("features/authentication/src/main/kotlin/$packagePath")).use { paths ->
        paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".kt") }
            .map(Files::readString)
            .toList()
            .joinToString("\n")
    }

    fun featureImports(): List<String> = Files.walk(root.resolve("features/authentication/src/main/kotlin/$packagePath")).use { paths ->
        paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".kt") }
            .flatMap { Files.lines(it) }
            .filter { it.startsWith("import ") }
            .toList()
    }
}
