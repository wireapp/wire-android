package com.wire.android.ui.authentication.create.overview

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreateAccountOverviewOwnershipTest {
    @Test
    fun `feature owns overview rendering and app only adapts host effects`() {
        val feature = source("features/authentication/src/main/kotlin/com/wire/android/ui/authentication/create/overview")
        val policy = source(
            "features/authentication/src/main/kotlin/com/wire/android/ui/authentication/create/common/CreateAccountFlowPolicy.kt",
        )
        val app = source("app/src/main/kotlin/com/wire/android/ui/authentication/create/overview/CreatePersonalAccountOverviewScreen.kt")
        assertTrue(feature.contains("fun CreateAccountOverviewContent("))
        assertTrue(feature.contains("class CreateAccountOverviewViewModel<LinksT>"))
        assertTrue(feature.contains("painterResource(id = overviewParams.contentIconResId)"))
        assertFalse(feature.contains("CreateAccountNavArgs"))
        assertFalse(feature.contains("com.wire.kalium"))
        assertTrue(policy.contains("val titleResId: Int"))
        assertTrue(policy.contains("val emailSubtitleResId: Int"))
        assertTrue(policy.contains("val isTeam: Boolean"))
        assertTrue(app.contains("val policy = flowType.createAccountFlowPolicy()"))
        assertTrue(app.contains("title = stringResource(policy.titleResId)"))
        assertTrue(app.contains("CustomTabsHelper.launchUrl"))
        assertFalse(app.contains("CreateAccountRouteFlowType.TEAM"))
    }

    private fun source(path: String): String = File(root(), path).walkTopDown().filter(File::isFile).joinToString("\n") { it.readText() }
    private fun root(): File = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
        .first { File(it, "app/src/main/kotlin").isDirectory }
}
