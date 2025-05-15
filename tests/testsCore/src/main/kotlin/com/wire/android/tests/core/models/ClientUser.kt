/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.tests.core.models

import com.wire.android.tests.core.config.Config
import com.wire.android.tests.core.services.backend.BackendConnections
import com.wire.android.tests.core.utils.MessagingUtils
import net.datafaker.Faker
import net.datafaker.providers.base.Text
import org.apache.commons.lang3.RandomStringUtils
import java.time.Duration
import java.util.concurrent.Callable
import net.datafaker.providers.base.Text.TextSymbolsBuilder.builder as textBuilder

class ClientUser {
    var accessCredentials: AccessCredentials? = null
    var id: String? = null
    var name: String
    var firstName: String
    var lastName: String
    var password: String
    var email: String
    private val _emailAliases: MutableSet<String> = HashSet()
    var emailPassword: String
    private val _nameAliases: MutableSet<String> = HashSet()
    private val _firstNameAliases: MutableSet<String> = HashSet()
    var uniqueUsername: String
    private val _uniqueUsernameAliases: MutableSet<String> = HashSet()
    private val _passwordAliases: MutableSet<String> = HashSet()
    var accentColor: AccentColor = AccentColor.Undefined
    var teamId: String? = null
    var expiresIn: Duration? = null
    var serviceProviderId: String? = null
    var SSO: Boolean = false
    var SCIM: Boolean = false
    var backendName: String? = null
    var hardcoded: Boolean = false
    var getUserIdThroughOwner: Callable<String>? = null
    private var _isTeamOwner: Boolean = false
    var registeredLegalHoldServices: Boolean = false
    var verificationCode: String? = null
    var activationCode: String? = null
    private val customSpecialSymbols = "!@#$"

    // Public read-only views of the mutable sets
    val emailAliases: Set<String> get() = HashSet(_emailAliases)
    val nameAliases: Set<String> get() = HashSet(_nameAliases)
    val firstNameAliases: Set<String> get() = HashSet(_firstNameAliases)
    val uniqueUsernameAliases: Set<String> get() = HashSet(_uniqueUsernameAliases)
    val passwordAliases: Set<String> get() = HashSet(_passwordAliases)

    // Properly encapsulated team owner property
    var isTeamOwner: Boolean
        get() = _isTeamOwner
        set(value) { _isTeamOwner = value }

    constructor() {
        val faker = Faker()
        firstName = faker.name().firstName()
        lastName = faker.name().lastName()
        // Reroll last name if it contains a quote to not break locator checks later
        while (lastName.contains("'")) {
            lastName = faker.name().lastName()
        }
        name = "$firstName $lastName"
        password = faker.text().text(
            textBuilder()
                .len(8)
                .with(Text.EN_LOWERCASE, 3)
                .with(Text.EN_UPPERCASE, 1)
                .with(customSpecialSymbols, 1)
                .with(Text.DIGITS, 1).build()
        )
        uniqueUsername = sanitizedRandomizedHandle(lastName)
        val connections = BackendConnections()
        email = if ( connections.getDefault().hasInbucketSetup()) {
            MessagingUtils.generateEmail(null, uniqueUsername)
        } else {
            MessagingUtils.generateEmail(MessagingUtils.getDefaultAccountName(), uniqueUsername)
        }
        emailPassword = Config.current().getDefaultEmailPassword(ClientUser::class.java)
    }

    constructor(firstName: String, lastName: String, email: String, password: String) {
        this.firstName = firstName
        this.lastName = lastName
        this.name = "$firstName $lastName"
        this.email = email
        this.password = password
        this.emailPassword = Config.current().getDefaultEmailPassword(ClientUser::class.java)
        this.uniqueUsername = sanitizedRandomizedHandle(lastName)
    }

    // Mutation methods for each set
    fun addEmailAlias(alias: String) = _emailAliases.add(alias)
    fun removeEmailAlias(alias: String) = _emailAliases.remove(alias)
    fun clearEmailAliases() = _emailAliases.clear()

    fun addNameAlias(alias: String) = _nameAliases.add(alias)
    fun removeNameAlias(alias: String) = _nameAliases.remove(alias)
    fun clearNameAliases() = _nameAliases.clear()

    fun addFirstNameAlias(alias: String) = _firstNameAliases.add(alias)
    fun removeFirstNameAlias(alias: String) = _firstNameAliases.remove(alias)
    fun clearFirstNameAliases() = _firstNameAliases.clear()

    fun addUniqueUsernameAlias(alias: String) = _uniqueUsernameAliases.add(alias)
    fun removeUniqueUsernameAlias(alias: String) = _uniqueUsernameAliases.remove(alias)
    fun clearUniqueUsernameAliases() = _uniqueUsernameAliases.clear()

    fun addPasswordAlias(alias: String) = _passwordAliases.add(alias)
    fun removePasswordAlias(alias: String) = _passwordAliases.remove(alias)
    fun clearPasswordAliases() = _passwordAliases.clear()

    fun getId(): String {
        if (id == null) {
            if (SSO) {
                try {
                    id = getUserIdThroughOwner?.call()
                } catch (e: Exception) {
                    throw IllegalStateException("No owner id; $e")
                }
            } else {
                val backendConnection = BackendConnections()
                id = backendConnection.get(backendName).getUserId(this)
            }
        }
        return id!!
    }

    fun getAccessCredentialsWithoutRefresh(): AccessCredentials? {
        return accessCredentials
    }

    companion object {
        fun sanitizedRandomizedHandle(derivative: String): String {
            return "${derivative}${generateRandomNumericString(8)}"
                .replace("[^A-Za-z0-9]".toRegex(), "")
                .lowercase()
        }

        private fun generateRandomNumericString(length: Int): String {
            return RandomStringUtils.randomNumeric(length)
        }
    }

    fun forceTokenExpiration() {
        accessCredentials = null
    }

    override fun toString(): String {
        return name
    }

    override fun equals(other: Any?): Boolean {
        return (other is ClientUser) && other.email == email
    }

    val isSSOUser: Boolean
        get() = SSO

    fun setUserIsSSOUser() {
        SSO = true
    }

    val isHarcoded: Boolean
        get() = hardcoded

    fun hasRegisteredLegalHoldService(): Boolean {
        return registeredLegalHoldServices
    }

    fun hasServiceProvider(): Boolean {
        return serviceProviderId != null
    }

    val isManagedBySCIM: Boolean
        get() = SCIM

    fun setManagedBySCIM() {
        SCIM = true
    }
}
