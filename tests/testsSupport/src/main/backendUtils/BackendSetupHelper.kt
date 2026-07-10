@file:Suppress("TooManyFunctions", "TooGenericExceptionThrown", "LongParameterList", "PackageNaming")
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
package backendUtils

import android.content.Context
import backendUtils.connection.acceptAllIncomingConnectionRequests
import backendUtils.connection.acceptIncomingConnectionRequest
import backendUtils.connection.sendConnectionRequest
import backendUtils.conversation.addUsersToGroupConversation
import backendUtils.conversation.createChannelTeamConversation
import backendUtils.conversation.createTeamConversation
import backendUtils.conversation.getConversationByName
import backendUtils.conversation.removeUserFromGroupConversation
import backendUtils.conversation.setArchivedStateForConversation
import backendUtils.team.addServiceToConversation
import backendUtils.team.enableChannelFeatureViaBackdoorTeam
import backendUtils.team.enableCellsFeatureViaBackdoorTeam
import backendUtils.team.enableForceAppLockFeature
import backendUtils.team.enableMLSFeatureTeam
import backendUtils.team.getTeamByName
import backendUtils.team.switchServiceForTeam
import backendUtils.team.TeamRoles
import backendUtils.team.unlockChannelFeature
import backendUtils.team.updateUniqueUsername
import backendUtils.user.createPersonalUserViaBackend
import kotlinx.coroutines.runBlocking
import service.enums.TeamService
import service.models.Conversation
import user.usermanager.ClientUserManager
import user.utils.ClientUser

/**
 * Test-facing helper for preparing backend state before UIAutomator continues in the app.
 */
class BackendSetupHelper(
    private val usersManager: ClientUserManager,
    private val addDevice: ((String, String?, String?) -> Unit)? = null
) {

    private fun backendFor(user: ClientUser): BackendClient {
        val backendName = user.backendName
        return if (backendName.isNullOrBlank()) {
            BackendClient.getDefault()
                ?: throw IllegalStateException("No default backend configured for user '${user.name}'.")
        } else {
            BackendClient.loadBackend(backendName)
        }
    }

    fun userXAddedContactsToGroupChat(
        userAsNameAlias: String,
        contactsToAddNameAliases: String,
        chatName: String
    ) {
        val userAs = toClientUser(userAsNameAlias)

        val contactsToAdd = usersManager
            .splitAliases(contactsToAddNameAliases)
            .map { toClientUser(it) }

        backendFor(userAs).addUsersToGroupConversation(
            asUser = userAs,
            contacts = contactsToAdd,
            conversation = toConvoObj(userAs, chatName)
        )
    }

    suspend fun usersSetUniqueUsername(userNameAliases: String) {
        usersManager.splitAliases(userNameAliases).forEach { userNameAlias ->
            val user = toClientUser(userNameAlias)
            val backend = backendFor(user)
            backend.updateUniqueUsername(
                user,
                user.uniqueUsername.orEmpty()
            )
        }
    }

    fun connectionRequestIsSentTo(userFromNameAlias: String, usersToNameAliases: String) {
        val userFrom = toClientUser(userFromNameAlias)
        val backend = backendFor(userFrom)
        val usersTo = usersManager
            .splitAliases(usersToNameAliases)
            .map(this::toClientUser)
        runBlocking {
            usersTo.forEach {
                backend.sendConnectionRequest(userFrom, it)
            }
        }
    }

    fun userIsConnectedTo(userFromNameAlias: String, usersToNameAliases: String) {
        val userFrom = toClientUser(userFromNameAlias)
        val fromBackend = backendFor(userFrom)
        val usersTo = usersManager
            .splitAliases(usersToNameAliases)
            .map(this::toClientUser)
        runBlocking {
            usersTo.forEach { userTo ->
                fromBackend.sendConnectionRequest(userFrom, userTo)
                backendFor(userTo).acceptIncomingConnectionRequest(userTo, userFrom)
            }
        }
    }

    fun createTeamOwnerByAlias(
        nameAlias: String,
        teamName: String,
        locale: String,
        updateHandle: Boolean,
        backendClient: BackendClient,
        context: Context
    ) {
        usersManager.createTeamOwnerByAlias(
            nameAlias,
            teamName,
            locale,
            updateHandle,
            backendClient,
            context
        )
    }

    fun createPersonalUser(user: ClientUser = ClientUser(), backendClient: BackendClient): ClientUser {
        val createdUser = backendClient.createPersonalUserViaBackend(user)
        createdUser.backendName = backendClient.name
        usersManager.appendCustomUser(createdUser)
        return createdUser
    }

    fun userAcceptsAllIncomingConnectionRequests(userNameAlias: String, backendClient: BackendClient? = null) {
        val user = toClientUser(userNameAlias)
        val backend = backendClient ?: backendFor(user)
        runBlocking {
            backend.acceptAllIncomingConnectionRequests(user)
        }
    }

    fun userConfiguresMLSForTeam(
        ownerUserAlias: String,
        teamName: String,
        backendClient: BackendClient
    ) {
        val owner = toClientUser(ownerUserAlias)
        runBlocking {
            val team = backendClient.getTeamByName(owner, teamName)
            backendClient.enableMLSFeatureTeam(
                team = team,
                defaultCipherSuite = 2,
                allowedCipherSuites = listOf(2),
                defaultProtocol = "mls",
                allowedProtocols = listOf("mls", "proteus")
            )
        }
    }

    fun userEnablesChannelFeatureForTeam(
        ownerUserAlias: String,
        teamName: String,
        backendClient: BackendClient
    ) {
        val owner = toClientUser(ownerUserAlias)
        runBlocking {
            val team = backendClient.getTeamByName(owner, teamName)
            backendClient.unlockChannelFeature(team)
            backendClient.enableChannelFeatureViaBackdoorTeam(team)
        }
    }

    fun enableForceAppLockFeature(
        ownerUserAlias: String,
        teamName: String,
        seconds: Int,
        backendClient: BackendClient
    ) {
        val owner = toClientUser(ownerUserAlias)
        runBlocking {
            val team = backendClient.getTeamByName(owner, teamName)
            backendClient.enableForceAppLockFeature(team, seconds)
        }
    }

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

    fun userEnablesServiceForTeam(ownerOrAdminUserAlias: String, serviceName: String, teamName: String) {
        userSwitchesServicesForTeam(ownerOrAdminUserAlias, true, serviceName, teamName)
    }

    fun userSwitchesServicesForTeam(
        ownerOrAdminUserAlias: String,
        isEnabled: Boolean,
        serviceNames: String,
        teamName: String
    ) {
        val ownerOrAdminUser = toClientUser(ownerOrAdminUserAlias)
        val backend = backendFor(ownerOrAdminUser)
        runBlocking {
            val team = backend.getTeamByName(ownerOrAdminUser, teamName)
            serviceNames.split(",")
                .map(String::trim)
                .map(TeamService::fromName)
                .forEach { service ->
                    backend.switchServiceForTeam(
                        ownerOrAdminUser,
                        team.id,
                        service.providerId,
                        service.serviceId,
                        isEnabled
                    )
                }
        }
    }

    fun userAddsBotToConversation(userWhoAddsAlias: String, botToAdd: String, chatName: String) {
        val userWhoAdds = toClientUser(userWhoAddsAlias)
        val backend = backendFor(userWhoAdds)
        val conversation = toConvoObj(userWhoAdds, chatName)
        runBlocking {
            backend.addServiceToConversation(userWhoAdds, botToAdd, conversation)
        }
    }

    fun userHasGroupConversationInTeam(
        chatOwnerNameAlias: String,
        chatName: String? = null,
        otherParticipantsNameAlises: String? = null,
        teamName: String,
        cellsEnabled: Boolean = false,
    ) {
        var participants: List<ClientUser>? = null
        val chatOwner = toClientUser(chatOwnerNameAlias)
        if (otherParticipantsNameAlises != null) {
            participants = usersManager
                .splitAliases(otherParticipantsNameAlises)
                .map(this::toClientUser)
        }

        val backend = backendFor(chatOwner)

        runBlocking {
            val dstTeam = backend.getTeamByName(chatOwner, teamName)
            backend.createTeamConversation(chatOwner, participants, chatName, dstTeam, cellsEnabled)
        }
    }

    fun enableCellsFeature(ownerUserAlias: String, teamName: String, backendClient: BackendClient) {
        val owner = toClientUser(ownerUserAlias)
        runBlocking {
            val team = backendClient.getTeamByName(owner, teamName)
            backendClient.enableCellsFeatureViaBackdoorTeam(team)
        }
    }

    fun userHasChannelConversationInTeam(
        chatOwnerNameAlias: String,
        chatName: String? = null,
        teamName: String
    ) {
        val chatOwner = toClientUser(chatOwnerNameAlias)
        val backend = backendFor(chatOwner)

        runBlocking {
            val dstTeam = backend.getTeamByName(chatOwner, teamName)
            backend.createChannelTeamConversation(chatOwner, chatName, dstTeam)
        }
    }

    fun userHas1on1ConversationInTeam(
        chatOwnerNameAlias: String,
        otherParticipantsNameAlises: String,
        teamName: String
    ) {
        val chatOwner = toClientUser(chatOwnerNameAlias)
        val participants = usersManager
            .splitAliases(otherParticipantsNameAlises)
            .map(this::toClientUser)
        val backend = backendFor(chatOwner)

        usersManager.splitAliases(otherParticipantsNameAlises).forEach { addDevice?.invoke(it, null, "Device1") }
        addDevice?.invoke(chatOwnerNameAlias, null, "Device1")

        runBlocking {
            val dstTeam = backend.getTeamByName(chatOwner, teamName)
            backend.createTeamConversation(chatOwner, participants, null, dstTeam)
        }
    }

    fun userArchivesConversation(
        userAlias: String,
        dstConvoName: String
    ) {
        val clientUser = toClientUser(userAlias)
        backendFor(clientUser).setArchivedStateForConversation(
            clientUser,
            toConvoObj(clientUser, dstConvoName),
            true
        )
    }

    fun userRemovesUserFromGroupConversation(
        userWhoRemovesAlias: String,
        userToRemoveAlias: String,
        chatName: String
    ) {
        val userWhoRemoves = toClientUser(userWhoRemovesAlias)
        val userToRemove = toClientUser(userToRemoveAlias)
        val backend = backendFor(userWhoRemoves)
        backend.removeUserFromGroupConversation(
            userWhoRemoves,
            userToRemove,
            toConvoObj(userWhoRemoves, chatName)
        )
    }

    private fun toClientUser(nameAlias: String): ClientUser {
        return usersManager.findUserByNameOrNameAlias(nameAlias)
    }

    private fun toConvoObj(owner: ClientUser, convoName: String): Conversation {
        val convoName = usersManager.replaceAliasesOccurrences(convoName, ClientUserManager.FindBy.NAME_ALIAS)
        val backend = backendFor(owner)
        return backend.getConversationByName(owner, convoName)
    }
}
