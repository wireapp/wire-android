///*
// * Wire
// * Copyright (C) 2025 Wire Swiss GmbH
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program. If not, see http://www.gnu.org/licenses/.
// */
//
// package user.utils
//
//import AccessCredentials
//import java.time.Duration
//import java.util.concurrent.Callable
//import java.util.HashSet
//import net.datafaker.Faker
//import net.datafaker.*
//import net.datafaker.providers.base.Text
//import net.datafaker.providers.base.Text.DIGITS
//import net.datafaker.providers.base.Text.EN_LOWERCASE
//import net.datafaker.providers.base.Text.EN_UPPERCASE
//
//class ClientUser {
//    var accessCredentials: AccessCredentials? = null
//    var id: String? = null
//    var name: String? = null
//    var firstName: String? = null
//    var lastName: String? = null
//    var password: String? = null
//    var email: String? = null
//    val emailAliases: MutableSet<String> = HashSet()
//    var emailPassword: String? = null
//    val nameAliases: MutableSet<String> = HashSet()
//    val firstNameAliases: MutableSet<String> = HashSet()
//    var uniqueUsername: String? = null
//    val uniqueUsernameAliases: MutableSet<String> = HashSet()
//    val passwordAliases: MutableSet<String> = HashSet()
////    var accentColor: AccentColor = AccentColor.Undefined
//    var teamId: String? = null
//    var expiresIn: Duration? = null
//    var serviceProviderId: String? = null
//    var SSO: Boolean = false
//    var SCIM: Boolean = false
//    var backendName: String? = null
//    var hardcoded: Boolean = false
//    var getUserIdThroughOwner: Callable<String>? = null
//    var isTeamOwner: Boolean = false
//    var registeredLegalHoldServices: Boolean = false
//    var verificationCode: String? = null
//    var activationCode: String? = null
//    val customSpecialSymbols: String = "!@#$"
//
//    constructor() {
//        val faker = Faker()
//        firstName = faker.name().firstName()
//        lastName = faker.name().lastName()
//        // Reroll last name if it contains a quote to not break locator checks later
//        while (lastName?.contains("'") == true) {
//            lastName = faker.name().lastName()
//        }
//        name = "$firstName $lastName"
//        password = faker.text().text(
//            Text.TextSymbolsBuilder.builder()
//            .len(8)
//            .with(EN_LOWERCASE, 3)
//            .with(EN_UPPERCASE, 1)
//            .with(customSpecialSymbols, 1)
//            .with(DIGITS, 1).build())
//        uniqueUsername = sanitizedRandomizedHandle(lastName)
//       // email =
//        //if (BackendConnections.getDefault() != null && BackendConnections.getDefault().hasInbucketSetup()) {
//          //  MessagingUtils.generateEmail(null, uniqueUsername)
//        //} else {
//          //  MessagingUtils.generateEmail(MessagingUtils.getDefaultAccountName(), uniqueUsername)
//       // }
//       // emailPassword = Config.current().getDefaultEmailPassword(ClientUser::class.java)
//    }
//
//    constructor(firstName: String, lastName: String, email: String, password: String) {
//        this.firstName = firstName
//        this.lastName = lastName
//        this.name = "$firstName $lastName"
//        this.email = email
//        this.password = password
//    }
//
//    fun getNameAliases(): Set<String> = HashSet(nameAliases)
//    fun getFirstNameAliases(): Set<String> = HashSet(firstNameAliases)
//    fun getPasswordAliases(): Set<String> = HashSet(passwordAliases)
//    fun getUniqueUsernameAliases(): Set<String> = HashSet(uniqueUsernameAliases)
//    fun getEmailAliases(): Set<String> = HashSet(emailAliases)
//
//    fun addNameAlias(alias: String) = nameAliases.add(alias)
//    fun removeNameAlias(alias: String) = nameAliases.remove(alias)
//    fun clearNameAliases() = nameAliases.clear()
//
//    fun addFirstNameAlias(alias: String) = firstNameAliases.add(alias)
//    fun removeFirstNameAlias(alias: String) = firstNameAliases.remove(alias)
//    fun clearFirstNameAliases() = firstNameAliases.clear()
//
//    fun addPasswordAlias(alias: String) = passwordAliases.add(alias)
//    fun removePasswordAlias(alias: String) = passwordAliases.remove(alias)
//    fun clearPasswordAliases() = passwordAliases.clear()
//
//    fun addUniqueUsernameAlias(alias: String) = uniqueUsernameAliases.add(alias)
//    fun removeUniqueUsernameAlias(alias: String) = uniqueUsernameAliases.remove(alias)
//    fun clearUniqueUsernameAliases() = uniqueUsernameAliases.clear()
//
//    fun addEmailAlias(alias: String) = emailAliases.add(alias)
//    fun removeEmailAlias(alias: String) = emailAliases.remove(alias)
//    fun clearEmailAliases() = emailAliases.clear()
//
//    fun getAccessCredentialsWithoutRefresh(): AccessCredentials? = accessCredentials
//
//    fun getId(): String? {
//        if (id == null) {
//            id = if (SSO) {
//                try {
//                    getUserIdThroughOwner?.call()
//                } catch (e: Exception) {
//                    throw IllegalStateException("No owner id; $e")
//                }
//            } else {
//                ""
//              //  BackendConnections.get(backendName).getUserId(this)
//            }
//        }
//        return id
//    }
//
//    fun setManagedBySCIM() { SCIM = true }
//    fun forceTokenExpiration() { accessCredentials = null }
//
//    override fun toString(): String = name ?: ""
//
//    override fun equals(other: Any?): Boolean =
//        (other is ClientUser) && other.email == email
//
//    fun isSSOUser(): Boolean = SSO
//    fun setUserIsSSOUser() { SSO = true }
//    fun isManagedBySCIM(): Boolean = SCIM
//    fun isHardcoded(): Boolean = hardcoded
//    fun hasRegisteredLegalHoldService(): Boolean = registeredLegalHoldServices
//    fun setRegisteredLegalHoldService(value: Boolean) { registeredLegalHoldServices = value }
//    fun hasServiceProvider(): Boolean = serviceProviderId != null
//
//    companion object {
//        fun sanitizedRandomizedHandle(derivative: String?): String =
//         //   "${derivative}${generateRandomNumericString(8)}"
//            "${derivative}${""}"
//                .replace("[^A-Za-z0-9]".toRegex(), "")
//                .lowercase()
//    }
//}
