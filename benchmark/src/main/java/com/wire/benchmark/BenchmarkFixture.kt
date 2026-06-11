/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.benchmark

import android.content.Context
import backendUtils.BackendClient
import backendUtils.team.TeamHelper
import backendUtils.team.TeamRoles
import backendUtils.team.deleteTeam
import backendUtils.user.removeBackendClients
import service.TestServiceHelper
import user.usermanager.ClientUserManager
import java.util.UUID

data class BenchmarkFixture(
    val email: String,
    val password: String,
    val conversationName: String,
    val backend: BackendClient,
    private val usersManager: ClientUserManager,
) {
    fun cleanup() {
        usersManager.createdUsers.forEach { user ->
            runCatching { user.removeBackendClients(backend) }
        }
        usersManager.getAllTeamOwners().forEach { owner ->
            runCatching { owner.deleteTeam(backend) }
        }
    }
}

object BenchmarkFixtureFactory {

    @Suppress("MagicNumber")
    fun create(
        backendName: String,
        context: Context,
        conversationNameOverride: String = "",
    ): BenchmarkFixture {
        val backend = BackendClient.loadBackend(backendName)
        val teamHelper = TeamHelper()
        val testServiceHelper = TestServiceHelper(teamHelper.usersManager)
        val suffix = UUID.randomUUID().toString().substring(0, 8)
        val teamName = "BaselineProfile$suffix"
        val conversationName = conversationNameOverride.ifEmpty { "BaselineConversation$suffix" }

        teamHelper.usersManager.createTeamOwnerByAlias(
            nameAlias = OWNER_ALIAS,
            teamName = teamName,
            locale = "en_US",
            updateHandle = true,
            backend = backend,
            context = context,
        )
        teamHelper.userXAddsUsersToTeam(
            ownerNameAlias = OWNER_ALIAS,
            userNameAliases = MEMBER_ALIAS,
            teamName = teamName,
            role = TeamRoles.Member,
            backendClient = backend,
            context = context,
            membersHaveHandles = true,
        )
        testServiceHelper.userHasGroupConversationInTeam(
            chatOwnerNameAlias = OWNER_ALIAS,
            chatName = conversationName,
            otherParticipantsNameAlises = MEMBER_ALIAS,
            teamName = teamName,
        )

        val owner = teamHelper.usersManager.findUserBy(OWNER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
        return BenchmarkFixture(
            email = owner.email.orEmpty(),
            password = owner.password.orEmpty(),
            conversationName = conversationName,
            backend = backend,
            usersManager = teamHelper.usersManager,
        )
    }

    private const val OWNER_ALIAS = "user1Name"
    private const val MEMBER_ALIAS = "user2Name"
}
