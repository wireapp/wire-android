/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.newauthentication.login

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class NewLoginResourceOwnershipTest {
    @Test
    fun `new login strings are feature owned with the original qualifier coverage`() {
        val root = repositoryRoot()
        val feature = root.resolve("features/authentication/src/main/res")
        val app = root.resolve("app/src/main/res")
        val featureMap = stringsByQualifier(feature)
        val appMap = stringsByQualifier(app)
        qualifierMap.forEach { (name, qualifiers) ->
            assertEquals(qualifiers, featureMap.filterValues { name in it }.keys, "Qualifier map changed for $name")
            assertFalse(appMap.values.any { name in it }, "App still owns feature resource: $name")
        }
    }

    private fun stringsByQualifier(resourceRoot: Path): Map<String, Set<String>> =
        Files.list(resourceRoot).use { directories ->
            directories.filter { Files.isDirectory(it) && it.fileName.toString().startsWith("values") }
                .toList()
                .associate { directory ->
                    directory.fileName.toString() to Files.list(directory).use { files ->
                        files.filter { it.fileName.toString().endsWith(".xml") }
                            .toList()
                            .flatMap { file -> resourceName.findAll(Files.readString(file)).map { it.groupValues[1] }.toList() }
                            .toSet()
                    }
                }
        }

    private fun repositoryRoot(): Path =
        generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("app/src/main/kotlin")) }

    private companion object {
        val resourceName = Regex("<string\\s+name=\\\"([^\\\"]+)\\\"")
        val allLocales = setOf("values", "values-de", "values-hu", "values-pt", "values-ru", "values-si", "values-tr")
        val withoutPt = allLocales - "values-pt"
        val emailLocales = setOf("values", "values-de", "values-hu", "values-ru", "values-si")
        val qualifierMap = mapOf(
            "content_description_enterprise_login_email_field" to allLocales,
            "content_description_login_email_field" to withoutPt,
            "enterprise_login_welcome" to allLocales,
            "enterprise_login_password_title" to allLocales,
            "enterprise_login_credentials_title" to allLocales,
            "enterprise_login_next" to allLocales,
            "enterprise_login_user_identifier_label" to allLocales,
            "enterprise_login_user_identifier_label_placeholder" to allLocales,
            "enterprise_login_error_invalid_user_identifier" to allLocales,
            "enterprise_login_create_account_label" to allLocales,
            "enterprise_login_create_account_text_button" to setOf("values", "values-de", "values-ru", "values-tr"),
            "enterprise_login_verification_code_title" to setOf("values", "values-de", "values-hu", "values-pt", "values-ru", "values-si"),
            "login_email_placeholder" to emailLocales,
            "login_email_label" to emailLocales,
            "login_error_invalid_email" to emailLocales,
        )
    }
}
