/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuthenticationPresentationOwnershipTest {

    @Test
    fun `feature owns remaining authentication dialog and server title renderers`() {
        val root = repositoryRoot()
        val featureSources = rendererSources.associateWith { relative ->
            root.resolve("features/authentication/src/main/kotlin/$relative")
        }
        featureSources.forEach { (relative, path) ->
            assertTrue(Files.isRegularFile(path), "Feature does not own $relative")
            val source = Files.readString(path)
            assertFalse(source.contains("com.wire.android.R"), relative)
            assertFalse(source.contains("com.wire.kalium"), relative)
        }

        assertFalse(Files.exists(root.resolve("app/src/main/kotlin/$ssoDialogPath")))
        val serverAdapter = Files.readString(root.resolve("app/src/main/kotlin/$serverTitlePath"))
        val loginMapper = Files.readString(root.resolve("app/src/main/kotlin/$loginMapperPath"))
        val failureAdapter = Files.readString(root.resolve("app/src/main/kotlin/$failureAdapterPath"))

        listOf(serverAdapter, loginMapper, failureAdapter).forEach { adapter ->
            assertFalse(adapter.contains("WireDialog("), "App adapter still renders a dialog")
            assertFalse(adapter.contains("InlineTextContent("), "App adapter still renders server title content")
        }
        assertTrue(serverAdapter.contains("ServerTitleContent("))
        assertTrue(loginMapper.contains("LoginDialogErrorData.Known"))
        assertTrue(loginMapper.contains("LoginDialogErrorData.Resolved"))
        assertTrue(failureAdapter.contains("AuthenticationFailureDialogContent("))
    }

    @Test
    fun `exclusive login and identity dialog resources moved exactly`() {
        val root = repositoryRoot()
        val appDefinitions = resourceDefinitions(root.resolve("app/src/main/res"))
        val featureDefinitions = resourceDefinitions(root.resolve("features/authentication/src/main/res"))

        assertTrue(appDefinitions.isEmpty(), "App still owns authentication dialog strings: $appDefinitions")
        assertEquals(175, featureDefinitions.size)
        assertEquals(
            expectedQualifiers,
            featureDefinitions
            .groupBy { definition -> resourceNames.single { definition.contains("name=\"$it\"") } }
            .mapValues { (_, definitions) -> definitions.map { it.substringBefore('|') }.toSet() }
        )
        assertEquals(
            "e78bded355894f8a46785a2b33fd25220c29b8a8e031506cd324117c3eb2d355",
            sha256(featureDefinitions.joinToString("\n")),
        )
    }

    private fun resourceDefinitions(root: Path): List<String> =
        Files.walk(root).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.parent.fileName.toString().startsWith("values") }
                .flatMap { path ->
                    val qualifier = path.parent.fileName.toString()
                    resourceRegex.findAll(Files.readString(path))
                        .map { "$qualifier|${it.value.trim()}" }
                        .toList()
                        .stream()
                }
                .sorted()
                .toList()
        }

    private fun repositoryRoot(): Path =
        generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("app/src/main/kotlin")) }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private companion object {
        const val serverTitlePath = "com/wire/android/ui/authentication/create/common/ServerTitle.kt"
        const val loginMapperPath = "com/wire/android/ui/authentication/login/LoginErrorDialogMapper.kt"
        const val ssoDialogPath = "com/wire/android/ui/authentication/login/sso/SsoIdentityChangedDialog.kt"
        const val failureAdapterPath =
            "com/wire/android/ui/authentication/devices/register/AuthenticationFailureDialog.kt"

        val rendererSources = setOf(
            "com/wire/android/ui/authentication/create/common/ServerTitleContent.kt",
            "com/wire/android/ui/authentication/login/LoginErrorDialog.kt",
            ssoDialogPath,
            "com/wire/android/ui/authentication/devices/register/AuthenticationFailureDialogContent.kt",
        )
        val resourceNames = setOf(
            "login_error_invalid_credentials_title", "login_error_invalid_credentials_message",
            "login_error_user_already_logged_in_title", "login_error_user_already_logged_in_message",
            "error_socket_title", "error_socket_message", "login_error_invalid_sso_code",
            "login_sso_error_invalid_cookie_title", "login_sso_error_invalid_cookie_message",
            "sso_error_dialog_title", "sso_error_dialog_message",
            "login_error_request_2fa_with_handle_title", "login_error_request_2fa_with_handle_message",
            "login_error_unauthorized_title", "login_error_unauthorized_message",
            "login_error_pending_activation_title", "login_error_pending_activation_message",
            "sso_identity_changed_dialog_title", "sso_identity_changed_dialog_message",
            "sso_identity_changed_dialog_confirm",
        )
        val resourceRegex = Regex(
            """<string\s+name="(${resourceNames.joinToString("|")})"[^>]*>.*?</string>""",
            RegexOption.DOT_MATCHES_ALL,
        )
        val commonLogin = setOf(
            "values", "values-de", "values-es", "values-fr", "values-hr", "values-hu", "values-it",
            "values-ja", "values-pl", "values-pt", "values-ru", "values-si", "values-sv",
        )
        val limitedAccount = setOf("values", "values-de", "values-hu", "values-pt", "values-ru", "values-si")
        val ssoResult = setOf(
            "values", "values-de", "values-es", "values-hu", "values-it", "values-pl", "values-pt",
            "values-ru", "values-si",
        )
        val expectedQualifiers = mapOf(
            "login_error_invalid_credentials_title" to commonLogin + "values-et",
            "login_error_invalid_credentials_message" to commonLogin,
            "login_error_user_already_logged_in_title" to commonLogin + "values-et",
            "login_error_user_already_logged_in_message" to commonLogin,
            "error_socket_title" to setOf(
                "values", "values-de", "values-es", "values-hr", "values-hu", "values-it", "values-pl",
                "values-pt", "values-ru", "values-si",
            ),
            "error_socket_message" to setOf(
                "values", "values-de", "values-es", "values-hu", "values-it", "values-pl", "values-pt",
                "values-ru", "values-si",
            ),
            "login_error_invalid_sso_code" to commonLogin,
            "login_sso_error_invalid_cookie_title" to commonLogin,
            "login_sso_error_invalid_cookie_message" to commonLogin,
            "sso_error_dialog_title" to ssoResult,
            "sso_error_dialog_message" to ssoResult,
            "login_error_request_2fa_with_handle_title" to limitedAccount,
            "login_error_request_2fa_with_handle_message" to limitedAccount,
            "login_error_unauthorized_title" to limitedAccount,
            "login_error_unauthorized_message" to limitedAccount,
            "login_error_pending_activation_title" to limitedAccount,
            "login_error_pending_activation_message" to limitedAccount,
            "sso_identity_changed_dialog_title" to setOf("values", "values-de", "values-ru"),
            "sso_identity_changed_dialog_message" to setOf("values", "values-de", "values-ru"),
            "sso_identity_changed_dialog_confirm" to setOf("values", "values-de", "values-ru"),
        )
    }
}
