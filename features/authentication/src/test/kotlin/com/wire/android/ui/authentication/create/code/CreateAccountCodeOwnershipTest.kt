/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.authentication.create.code

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreateAccountCodeOwnershipTest {
    @Test
    fun givenCodeStateEngine_thenFeatureOwnsViewModelStateGatewayAndResult() {
        val root = repositoryRoot()
        sourceFiles.forEach { source ->
            assertFalse(Files.exists(root.resolve("app/src/main/kotlin/$packagePath/$source")))
            assertTrue(Files.isRegularFile(root.resolve("features/authentication/src/main/kotlin/$packagePath/$source")))
        }
        assertTrue(Files.isRegularFile(root.resolve("app/src/main/kotlin/$packagePath/CreateAccountCodeViewModelHostFactory.kt")))
        assertTrue(Files.isRegularFile(root.resolve("app/src/main/kotlin/com/wire/android/ui/registration/code/CreateAccountVerificationCodeViewState.kt")))
    }

    @Test
    fun givenFeatureCodeSources_thenHostTypesAndResourcesDoNotCrossBoundary() {
        val root = repositoryRoot()
        val sourceRoot = root.resolve("features/authentication/src/main/kotlin/$packagePath")
        val source = Files.walk(sourceRoot).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".kt") }
                .map(Files::readString)
                .toList()
                .joinToString("\n")
        }
        forbidden.forEach { value -> assertFalse(source.contains(value), "Feature code source contains $value") }
        assertTrue(source.contains("class CreateAccountCodeViewModel<FlowT, LinksT, FailureT, UserT, CredentialsT>"))
        assertTrue(source.contains("const val RESEND_TIMER_DELAY = 300L"))
        assertTrue(source.contains("val codeLength: Int = DEFAULT_VERIFICATION_CODE_LENGTH"))
    }

    @Test
    fun givenCodePresentation_whenInspectingOwners_thenFeatureRendersAndAppOnlyAdapts() {
        val root = repositoryRoot()
        val featureContent = Files.readString(
            root.resolve("features/authentication/src/main/kotlin/$packagePath/CreateAccountCodeContent.kt")
        )
        val appScreen = Files.readString(
            root.resolve("app/src/main/kotlin/$packagePath/CreateAccountCodeScreen.kt")
        )

        assertTrue(featureContent.contains("data class CreateAccountCodePresentation("))
        assertTrue(featureContent.contains("fun <FlowT, UserT, FailureT> CreateAccountCodeContent("))
        assertTrue(featureContent.contains("modifier: Modifier = Modifier"))
        assertTrue(featureContent.contains("WireScaffold("))
        assertTrue(featureContent.contains("CodeTextField("))
        assertTrue(featureContent.contains("InvalidActivationCodeError"))
        assertTrue(featureContent.contains("AnimatedVisibility(visible = state.loading)"))
        assertTrue(featureContent.contains("clickEnabled = !state.loading"))
        assertTrue(featureContent.contains("timerText = state.remainingTimerText"))
        assertTrue(featureContent.contains("focusRequester.requestFocus()"))
        assertTrue(featureContent.contains("keyboardController?.show()"))
        listOf(
            "com.wire.android.R",
            "CreateAccountFlowType",
            "CreateAccountNavArgs",
            "ServerConfig",
            "CoreFailure",
            "DialogErrorStrings",
            "CustomTabs",
        ).forEach { value -> assertFalse(featureContent.contains(value), "Feature presentation contains $value") }

        assertTrue(appScreen.contains("CreateAccountCodeContent("))
        assertTrue(appScreen.contains("CreateAccountCodePresentation("))
        assertTrue(appScreen.contains("ServerTitle("))
        assertTrue(appScreen.contains("WireDialog("))
        assertTrue(appScreen.contains("CreateAccountSummaryNavArgs(flowType)"))
        assertFalse(appScreen.contains("WireScaffold("))
        assertFalse(appScreen.contains("CodeTextField("))
        assertFalse(appScreen.contains("FocusRequester"))
    }

    @Test
    fun givenFeatureCodeSources_whenInspectingImports_thenAppAndHostDependenciesAreExcluded() {
        val root = repositoryRoot()
        val sourceRoot = root.resolve("features/authentication/src/main/kotlin/$packagePath")
        val imports = Files.walk(sourceRoot).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".kt") }
                .flatMap { Files.lines(it) }
                .filter { it.startsWith("import ") }
                .toList()
        }

        listOf(
            "com.wire.android.R",
            "CreateAccountFlowType",
            "CreateAccountNavArgs",
            "ServerConfig",
            "CoreFailure",
            "com.wire.kalium",
            "dev.zacsweers.metro",
            "DialogErrorStrings",
            "CustomTabs",
        ).forEach { forbiddenImport ->
            assertFalse(
                imports.any { it.contains(forbiddenImport) },
                "Forbidden feature dependency: $forbiddenImport",
            )
        }
    }

    @Test
    fun givenCodeResults_whenInspectingAppAdapter_thenDialogAndNavigationGuardsRemainAppOwnedAndOrdered() {
        val root = repositoryRoot()
        val appScreen = Files.readString(
            root.resolve("app/src/main/kotlin/$packagePath/CreateAccountCodeScreen.kt")
        )

        assertTrue(appScreen.contains("(codeState.result as? CreateAccountCodeResult.Error.DialogError)?.let"))
        assertTrue(appScreen.contains("(codeState.result as? CreateAccountCodeResult.Success)?.let"))
        assertTrue(appScreen.contains("CreateAccountCodeResult.Error.TooManyDevicesError"))
        val tooManyDevicesBlock = appScreen.substringAfter("if (tooManyDevicesError != null)")
        val clearError = tooManyDevicesBlock.indexOf("clearCodeError()")
        val clearField = tooManyDevicesBlock.indexOf("clearCodeField()")
        val navigateToDevices = tooManyDevicesBlock.indexOf("onTooManyDevices(tooManyDevicesError.userId)")
        assertTrue(clearError >= 0 && clearError < clearField && clearField < navigateToDevices)
    }

    private fun repositoryRoot(): Path =
        generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("app/src/main/kotlin")) }

    private companion object {
        const val packagePath = "com/wire/android/ui/authentication/create/code"
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
    }
}
