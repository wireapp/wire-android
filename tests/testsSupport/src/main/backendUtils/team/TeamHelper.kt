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
@file:Suppress("PackageNaming")
package backendUtils.team

import android.content.Context
import backendUtils.BackendClient
import kotlinx.coroutines.runBlocking
import user.usermanager.ClientUserManager
import user.utils.ClientUser

class TeamHelper {
    val usersManager: ClientUserManager = ClientUserManager(useSpecialEmail = false)

    @Suppress("LongParameterList", "TooGenericExceptionThrown")
    fun userXAddsUsersToTeam(
        ownerNameAlias: String,
        userNameAliases: String,
        teamName: String,
        role: TeamRoles,
        backendClient: BackendClient,
        context: Context,
        membersHaveHandles: Boolean
    ) {
        val admin = toClientUser(ownerNameAlias)
        val dstTeam = runBlocking { backendClient.getTeamByName(admin, teamName) }
        val membersToBeAdded = mutableListOf<ClientUser>()
        val aliases = usersManager.splitAliases(userNameAliases)
        for (userNameAlias in aliases) {
            val user = toClientUser(userNameAlias)
            if (usersManager.isUserCreated(user)) {
                throw Exception(
                    "Cannot add user with alias $userNameAlias to team because user is already created"
                )
            }
            membersToBeAdded.add(user)
        }
        usersManager.createTeamMembers(
            teamOwner = admin,
            teamId = dstTeam.id,
            members = membersToBeAdded,
            membersHaveHandles = membersHaveHandles,
            role = role,
            backend = backendClient,
            context = context
        )
    }

    private fun toClientUser(nameAlias: String): ClientUser {
        return usersManager.findUserByNameOrNameAlias(nameAlias)
    }
}
