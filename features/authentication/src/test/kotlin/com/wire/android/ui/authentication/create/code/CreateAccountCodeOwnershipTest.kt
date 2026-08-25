package com.wire.android.ui.authentication.create.code

import java.nio.file.Files
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreateAccountCodeOwnershipTest {
    @Test
    fun givenCodeStateEngine_thenFeatureOwnsViewModelStateGatewayAndResult() {
        CreateAccountCodeOwnershipFixtures.sourceFiles.forEach { source ->
            assertFalse(Files.exists(CreateAccountCodeOwnershipFixtures.appSource(source)))
            assertTrue(Files.isRegularFile(CreateAccountCodeOwnershipFixtures.featureSource(source)))
        }
        assertTrue(
            Files.isRegularFile(
                CreateAccountCodeOwnershipFixtures.appSource("CreateAccountCodeViewModelHostFactory.kt"),
            ),
        )
        assertFalse(
            Files.exists(
                CreateAccountCodeOwnershipFixtures.root.resolve(
                    "app/src/main/kotlin/com/wire/android/ui/registration/code/CreateAccountVerificationCodeViewState.kt",
                ),
            ),
        )
    }

    @Test
    fun givenFeatureCodeSources_thenHostTypesAndResourcesDoNotCrossBoundary() {
        val source = CreateAccountCodeOwnershipFixtures.featureCodeSource()

        CreateAccountCodeOwnershipFixtures.forbidden.forEach { value ->
            assertFalse(source.contains(value), "Feature code source contains $value")
        }
        assertTrue(source.contains("class CreateAccountCodeViewModel<FlowT, LinksT, FailureT, UserT, CredentialsT>"))
        assertTrue(source.contains("const val RESEND_TIMER_DELAY = 300L"))
        assertTrue(source.contains("val codeLength: Int = DEFAULT_VERIFICATION_CODE_LENGTH"))
    }

    @Test
    fun givenFeatureCodeSources_whenInspectingImports_thenAppAndHostDependenciesAreExcluded() {
        val imports = CreateAccountCodeOwnershipFixtures.featureImports()

        CreateAccountCodeOwnershipFixtures.forbiddenImports.forEach { forbiddenImport ->
            assertFalse(
                imports.any { it.contains(forbiddenImport) },
                "Forbidden feature dependency: $forbiddenImport",
            )
        }
    }

    @Test
    fun `code engine is feature-owned and app preserves typed completion`() {
        val feature = CreateAccountCodeOwnershipFixtures.featureCodeSource()
        val app = Files.readString(CreateAccountCodeOwnershipFixtures.appSource("CreateAccountCodeScreen.kt"))
        val factory = Files.readString(
            CreateAccountCodeOwnershipFixtures.appSource("CreateAccountCodeViewModelHostFactory.kt"),
        )
        assertTrue(feature.contains("class CreateAccountCodeViewModel<FlowT, LinksT, FailureT, UserT, CredentialsT>"))
        assertTrue(feature.contains("fun <FlowT, UserT, FailureT> CreateAccountCodeContent("))
        assertTrue(feature.contains("CreateAccountRegistrationRequest.Team"))
        assertTrue(feature.contains("CodeTextField("))
        assertTrue(feature.contains("ResendCodeText("))
        assertTrue(feature.contains("LaunchedEffect(Unit)"))
        assertFalse(feature.contains("CreateAccountNavArgs"))
        assertFalse(feature.contains("com.wire.kalium"))
        assertFalse(feature.contains("WireDialog("))
        assertTrue(factory.contains("type.createAccountFlowPolicy().isTeam"))
        assertTrue(app.contains("CreateAccountCodeContent("))
        assertTrue(app.contains("onSuccess(flowType, it.userId)"))
        assertTrue(app.contains("WireDialog("))
        assertTrue(app.indexOf("clearCodeError()") < app.indexOf("onTooManyDevices("))
    }
}
