/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class AuthenticationExclusiveResourceOwnershipTest {

    @Test
    fun givenAuthenticationExclusiveStrings_thenOnlyFeatureOwnsEveryQualifiedValueVerbatim() {
        val root = repositoryRoot()
        assertEquals(emptyList<String>(), definitions(root.resolve("app/src/main/res")))
        val featureDefinitions = definitions(root.resolve("features/authentication/src/main/res"))
        assertEquals(211, featureDefinitions.size)
        assertEquals("73a992ed61cc5c60090d793d6236ebac37a51545ce9ad6407adfcd07bdefff1d", sha256(featureDefinitions.joinToString("\n")))
    }

    @Test
    fun givenAppAuthenticationHosts_thenTheyResolveExclusiveStringsFromAuthenticationR() {
        val root = repositoryRoot()
        hostSources.forEach { source ->
            val text = Files.readString(root.resolve(source))
            resourceNames.filter { text.contains("$it") }.forEach { resource ->
                assertFalse(Regex("(?<![A-Za-z])R\\.string\\.$resource\\b").containsMatchIn(text), "$source still uses app R for $resource")
            }
        }
    }

    private fun definitions(resourceRoot: Path): List<String> =
        Files.walk(resourceRoot).use { paths ->
            paths.filter { it.fileName.toString().endsWith(".xml") }
                .flatMap { path ->
                    resourceRegex.findAll(Files.readString(path)).map { "${path.parent.fileName}|${it.value.trim()}" }.toList().stream()
                }.sorted().toList()
        }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }

    private fun repositoryRoot(): Path = generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
        .first { Files.isDirectory(it.resolve("app/src/main/kotlin")) }

    private companion object {
        val resourceNames = setOf(
            "login_title", "content_description_login_back_btn", "content_description_login_user_identifier_field",
            "content_description_login_password_field", "sso_not_supported_dialog_description", "missing_backend_config_title",
            "missing_backend_config_error", "label_wire_credentials", "login_user_identifier_label",
            "login_error_invalid_user_identifier", "login_forgot_password", "label_proxy_credentials",
            "proxy_credential_description", "welcome_footer_text", "welcome_button_create_personal_account",
            "content_description_welcome_screen_close_btn", "create_team_not_supported_dialog_description",
            "create_personal_account_not_supported_dialog_description", "create_account_username_placeholder",
            "create_account_username_label", "create_account_username_description", "create_account_username_taken_error",
        )
        val resourceRegex = Regex(
            """<string\s+name="(${resourceNames.joinToString("|")})"[^>]*>.*?</string>""",
            setOf(RegexOption.DOT_MATCHES_ALL),
        )
        val hostSources = listOf(
            "app/src/main/kotlin/com/wire/android/ui/authentication/login/LoginMainContent.kt",
            "app/src/main/kotlin/com/wire/android/ui/authentication/login/email/LoginEmailScreen.kt",
            "app/src/main/kotlin/com/wire/android/ui/authentication/welcome/WelcomeContent.kt",
            "app/src/main/kotlin/com/wire/android/ui/authentication/welcome/WelcomeAccountCreation.kt",
            "app/src/main/kotlin/com/wire/android/ui/authentication/create/username/CreateAccountUsernameScreen.kt",
            "app/src/main/kotlin/com/wire/android/ui/authentication/create/common/handle/UsernameTextField.kt",
            "app/src/main/kotlin/com/wire/android/ui/authentication/create/code/CreateAccountCodeScreen.kt",
            "app/src/main/kotlin/com/wire/android/ui/authentication/create/overview/CreatePersonalAccountOverviewScreen.kt",
            "app/src/main/kotlin/com/wire/android/ui/authentication/BackendConfigHostContent.kt",
            "app/src/main/kotlin/com/wire/android/ui/newauthentication/login/NewLoginHeader.kt",
            "app/src/main/kotlin/com/wire/android/ui/newauthentication/login/NewLoginScreen.kt",
            "app/src/main/kotlin/com/wire/android/ui/newauthentication/login/password/NewLoginPasswordScreen.kt",
        )
    }
}
