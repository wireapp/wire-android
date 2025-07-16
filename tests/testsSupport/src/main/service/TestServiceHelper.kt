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
package service

import com.wire.android.testSupport.backendConnections.BackendClient
import com.wire.android.testSupport.backendConnections.team.getTeamByName
import com.wire.android.testSupport.service.TestService
import kotlinx.coroutines.runBlocking
import network.HttpRequestException
import user.usermanager.ClientUserManager
import user.utils.ClientUser
import java.util.concurrent.TimeUnit

class TestServiceHelper {

    val usersManager by lazy {
        ClientUserManager.getInstance()!!
    }

    val testServiceClient by lazy {
        TestService("","TestService")
    }

    suspend fun usersSetUniqueUsername(userNameAliases: String) {
        usersManager.splitAliases(userNameAliases).forEach { userNameAlias ->
            val user = toClientUser(userNameAlias)
            val backend = BackendClient.loadBackend(user.backendName.orEmpty())
            backend.updateUniqueUsername(user, user.uniqueUsername.orEmpty())
        }
    }

    fun connectionRequestIsSentTo(userFromNameAlias: String, usersToNameAliases: String) {
        val userFrom = toClientUser(userFromNameAlias);
        val backend = BackendClient.loadBackend(userFrom.backendName.orEmpty());
        val usersTo = usersManager
            .splitAliases(usersToNameAliases)
            .map(this::toClientUser)
        runBlocking {
            usersTo.forEach {
                backend.sendConnectionRequest(userFrom, it)
            }
        }
    }
    fun addDevice(
        ownerAlias: String,
        verificationCode: String? = null,
        deviceName: String? = null,
        label: String? = "",
        developmentApiEnabled: Boolean = false
    ) {
        try {
            testServiceClient.login(
                toClientUser(ownerAlias),
                verificationCode,
                deviceName,
                developmentApiEnabled
            )
        } catch (e: HttpRequestException) {
            try {
                TimeUnit.SECONDS.sleep(300)
                testServiceClient.login(
                    toClientUser(ownerAlias),
                    verificationCode,
                    deviceName,
                    developmentApiEnabled
                )
            } catch (ei: Exception) {
                throw RuntimeException("Wait and retry failed")
            }
        }
    }

    fun userHasGroupConversationInTeam(
        chatOwnerNameAlias: String,
        chatName: String? = null,
        otherParticipantsNameAlises: String? = null,
        teamName: String
    ) {
        var participants: List<ClientUser>? = null
        val chatOwner = toClientUser(chatOwnerNameAlias)
        if (otherParticipantsNameAlises != null) {
            participants = usersManager
                .splitAliases(otherParticipantsNameAlises)
                .map(this::toClientUser)
        }

        val backend = if (chatOwner.backendName.isNullOrEmpty()) {
            BackendClient.getDefault()
        } else {
            BackendClient.loadBackend(chatOwner.backendName.orEmpty())
        }

        runBlocking {
            val dstTeam = backend?.getTeamByName(chatOwner, teamName)
            backend?.createTeamConversation(chatOwner, participants, chatName, dstTeam!!)
        }
    }

    fun toClientUser(nameAlias: String): ClientUser {
        return usersManager.findUserByNameOrNameAlias(nameAlias)
    }
}
