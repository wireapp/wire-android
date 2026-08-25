/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.create.code

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreateAccountCodePresentationOwnershipTest {
    @Test
    fun givenCodePresentation_whenInspectingOwners_thenFeatureRendersAndAppOnlyAdapts() {
        val featureContent = CreateAccountCodeOwnershipFixtures.featureSource("CreateAccountCodeContent.kt").toFile().readText()
        val appScreen = CreateAccountCodeOwnershipFixtures.appSource("CreateAccountCodeScreen.kt").toFile().readText()

        listOf(
            "data class CreateAccountCodePresentation(",
            "fun <FlowT, UserT, FailureT> CreateAccountCodeContent(",
            "modifier: Modifier = Modifier",
            "WireScaffold(",
            "CodeTextField(",
            "InvalidActivationCodeError",
            "AnimatedVisibility(visible = state.loading)",
            "clickEnabled = !state.loading",
            "timerText = state.remainingTimerText",
            "focusRequester.requestFocus()",
            "keyboardController?.show()",
        ).forEach { value -> assertContains(featureContent, value) }
        listOf(
            "com.wire.android.R",
            "CreateAccountFlowType",
            "CreateAccountNavArgs",
            "ServerConfig",
            "CoreFailure",
            "DialogErrorStrings",
            "CustomTabs",
        ).forEach { value -> assertFalse(featureContent.contains(value), "Feature presentation contains $value") }

        listOf(
            "CreateAccountCodeContent(",
            "CreateAccountCodePresentation(",
            "ServerTitle(",
            "WireDialog(",
            "onSuccess(flowType, it.userId)",
        ).forEach { value -> assertContains(appScreen, value) }
        assertFalse(appScreen.contains("CreateAccountSummaryNavArgs"))
        listOf("WireScaffold(", "CodeTextField(", "FocusRequester").forEach { value ->
            assertFalse(appScreen.contains(value))
        }
    }

    @Test
    fun givenCodeResults_whenInspectingAppAdapter_thenDialogAndNavigationGuardsRemainAppOwnedAndOrdered() {
        val appScreen = CreateAccountCodeOwnershipFixtures.appSource("CreateAccountCodeScreen.kt").toFile().readText()

        assertTrue(appScreen.contains("(codeState.result as? CreateAccountCodeResult.Error.DialogError)?.let"))
        assertTrue(appScreen.contains("(codeState.result as? CreateAccountCodeResult.Success)?.let"))
        assertTrue(appScreen.contains("CreateAccountCodeResult.Error.TooManyDevicesError"))
        val tooManyDevicesBlock = appScreen.substringAfter("if (tooManyDevicesError != null)")
        val clearError = tooManyDevicesBlock.indexOf("clearCodeError()")
        val clearField = tooManyDevicesBlock.indexOf("clearCodeField()")
        val navigateToDevices = tooManyDevicesBlock.indexOf("onTooManyDevices(tooManyDevicesError.userId)")
        assertTrue(clearError >= 0 && clearError < clearField && clearField < navigateToDevices)
    }

    private fun assertContains(source: String, value: String) = assertTrue(source.contains(value), "Missing $value")
}
