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
@file:Suppress("TooManyFunctions")
package user.utils.component

import user.utils.ClientUser
import user.utils.AccessCredentials

fun ClientUser.addNameAlias(alias: String) = nameAliases.add(alias)
fun ClientUser.removeNameAlias(alias: String) = nameAliases.remove(alias)
fun ClientUser.clearNameAliases() = nameAliases.clear()

fun ClientUser.addFirstNameAlias(alias: String) = firstNameAliases.add(alias)
fun ClientUser.removeFirstNameAlias(alias: String) = firstNameAliases.remove(alias)
fun ClientUser.clearFirstNameAliases() = firstNameAliases.clear()

fun ClientUser.addPasswordAlias(alias: String) = passwordAliases.add(alias)
fun ClientUser.removePasswordAlias(alias: String) = passwordAliases.remove(alias)
fun ClientUser.clearPasswordAliases() = passwordAliases.clear()

fun ClientUser.addUniqueUsernameAlias(alias: String) = uniqueUsernameAliases.add(alias)
fun ClientUser.removeUniqueUsernameAlias(alias: String) = uniqueUsernameAliases.remove(alias)
fun ClientUser.clearUniqueUsernameAliases() = uniqueUsernameAliases.clear()

fun ClientUser.addEmailAlias(alias: String) = emailAliases.add(alias)
fun ClientUser.removeEmailAlias(alias: String) = emailAliases.remove(alias)
fun ClientUser.clearEmailAliases() = emailAliases.clear()

fun ClientUser.getAccessCredentialsWithoutRefresh(): AccessCredentials? = accessCredentials

@Suppress("TooGenericExceptionCaught")
fun ClientUser.getUserId(): String? {
    if (id == null) {
        id = if (sso) {
            try {
                getUserIdThroughOwner?.call()
            } catch (e: Exception) {
                throw IllegalStateException("No owner id; $e")
            }
        } else {
            ""
            //  BackendConnections.get(backendName).getUserId(this)
        }
    }
    return id
}

fun ClientUser.setManagedBySCIM() {
    scim = true
}

fun ClientUser.forceTokenExpiration() {
    accessCredentials = null
}
