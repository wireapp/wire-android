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
package com.wire.android.tests.core.services

import com.wire.android.tests.core.config.Config
import com.wire.android.tests.core.models.ClientUser
import com.wire.android.tests.core.services.backend.BackendConnections
import com.wire.android.tests.core.stripe.StripeAPIClient
import com.wire.android.tests.core.utils.OktaAPIClient
import com.wire.android.tests.core.utils.Timedelta
import com.wire.android.tests.core.utils.ZetaLogger
import org.json.JSONObject
import java.awt.*
import java.io.File
import java.time.Duration
import java.util.*
import java.util.concurrent.*
import java.util.logging.Logger

class AndroidTestSteps(private val usersManager: ClientUsersManager) {
    companion object {
        val DEFAULT_WAIT_UNTIL_INTERVAL: Timedelta = Timedelta.ofMillis(1000.0)
        val DEFAULT_WAIT_UNTIL_TIMEOUT: Timedelta = Timedelta.ofSeconds(10.0)
        private const val DEFAULT_LOCALE = "en_US"
        private val CUSTOMER_WAIT_UNTIL_INTERVAL: Timedelta = Timedelta.ofMillis(5000.0)
        private val CUSTOMER_WAIT_UNTIL_TIMEOUT: Timedelta = Timedelta.ofSeconds(240.0)
        const val DEFAULT_AUTOMATION_MESSAGE = "1 message"
        private val log: Logger = ZetaLogger.getLog(AndroidTestSteps::class.simpleName)
        private val BACKEND_USER_SYNC_TIMEOUT: Timedelta = Timedelta.ofSeconds(180.0)
        private val PICTURE_CHANGE_TIMEOUT: Timedelta = Timedelta.ofSeconds(15.0)
        private const val USER_DETAIL_NOT_SET = "NOT_SET"
        private val stripeAPIClient: StripeAPIClient = StripeAPIClient()
        private val mixPanelClient: MixPanelAPIClient = MixPanelMockAPIClient()
        private val MIXPANEL_TIMEOUT: Duration = Duration.ofMinutes(10)
        const val WIRE_RECEIPT_MODE = "WIRE_RECEIPT_MODE"
        private var lastMixpanelResponse: JSONObject = JSONObject()
        private var lastMixpanelDistrictId: String = ""
        private var lastMixpanelEvent: String = ""
        val FIRST_AVAILABLE_DEVICE: String? = null
        val NO_EXPIRATION: Timedelta = Timedelta.ofSeconds(0.0)
        var KUBECTLPATH: String = "/usr/local/bin/"
            private set

        init {
            if (System.getenv("WORKSPACE") != null) {
                KUBECTLPATH = System.getenv("WORKSPACE") + File.separator
            }
        }

        fun createJsonFromMapping(mappingAsJson: Array<Array<String>>): JSONObject {
            val json = JSONObject()
            mappingAsJson.forEach { e -> json.put(e[0], e[1]) }
            return json
        }
    }

    private val oktaAPIClient: OktaAPIClient? = null
    private val keycloakAPIClient: KeycloakAPIClient
    private var testServiceClient: TestServiceClient? = null
    private val scimClient: ScimClient
    private val profilePictureV3SnapshotsMap: MutableMap<String, String> = HashMap()
    private val profilePictureV3PreviewSnapshotsMap: MutableMap<String, String> = HashMap()
    private val recentMessageIds: MutableMap<String, Optional<String>> = HashMap()
    private var identityProviderId: String? = null
    private val customDomains: MutableList<String> = ArrayList()
    val defederatedBackends: MutableMap<String, String> = HashMap()
    val touchedFederator: MutableList<String> = ArrayList()
    val touchedBrig: MutableList<String> = ArrayList()
    val touchedGalley: MutableList<String> = ArrayList()
    val touchedIngress: MutableList<String> = ArrayList()
    val touchedSFT: MutableList<String> = ArrayList()
    private var isOldTestService: Boolean = false

    init {
        val defaultBackendName = Config.common().getBackendType(CommonUtils::class.java)
        keycloakAPIClient = KeycloakAPIClient(defaultBackendName)
        useNewTestService()
        scimClient = ScimClient(defaultBackendName)
    }

    fun cleanUpBackends() {
        log.fine("Clean up push tokens from the backends")
        usersManager.createdUsers.parallelStream().forEach { user ->
            try {
                val backend = BackendConnections.get(user)
                backend.getOtrClients(user).parallelStream().forEach { c ->
                    try {
                        backend.removeOtrClient(user, c)
                    } catch (e: Exception) {
                        log.fine(
                            String.format(
                                "Could not remove client for user %s: %s",
                                user.name,
                                e.message
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                log.fine("Issue on backend cleanup: " + e.message)
            }
        }
        log.fine("Deactivate LH devices and delete teams from backends")
        usersManager.allTeamOwners.forEach { user ->
            val backend = BackendConnections.get(user)
            if (!user.isHarcoded) {
                if (user.hasRegisteredLegalHoldService()) {
                    try {
                        log.fine("Unregister LH for team with id " + user.teamId)
                        backend.unregisterLegalHoldService(user, user.teamId)
                    } catch (e: Exception) {
                        log.fine("Problem with getting team members: " + e.message)
                    }
                }
                try {
                    for (team in backend.getAllTeams(user)) {
                        backend.deleteTeam(user, team)
                    }
                } catch (e: Exception) {
                    log.fine(
                        String.format(
                            "Error while deleting teams for owner '%s': %s",
                            user.name,
                            e.message
                        )
                    )
                }
            } else {
                log.fine("Delete all no-owner members of hardcoded team from backends")
                // If team owner is hardcoded we delete all members
                val members = backend.getTeamMembers(user)
                val teamId = backend.getAllTeams(user)[0].id
                for (member in members) {
                    try {
                        backend.deleteTeamMember(user, teamId, member.userId)
                    } catch (e: Exception) {
                        log.severe("Could not delete team member " + member.userId)
                    }
                }
                log.fine("Logout team owner to remove cookie and avoid being banned")
                backend.logout(user)
            }
        }
        usersManager.allTeamOwners.clear()
        log.fine("Delete custom domains")
        for (customDomain in customDomains) {
            deleteCustomBackend(customDomain)
        }
    }

    private fun getUsersManager(): ClientUsersManager {
        return usersManager
    }

    private fun toClientUser(nameAlias: String): ClientUser {
        return usersManager.findUserByNameOrNameAlias(nameAlias)
    }

    private fun toConvoId(ownerAlias: String, convoName: String): String {
        return toConvoObj(ownerAlias, convoName).id
    }

    private fun toConvoId(owner: ClientUser, convoName: String): String {
        return toConvoObj(owner, convoName).id
    }

    fun getConversationMessageTimer(member: ClientUser, convoName: String): Int {
        return toConvoObj(member, convoName).messageTimerInMilliseconds
    }

    private fun toConvoObj(ownerAlias: String, convoName: String): Conversation {
        return toConvoObj(toClientUser(ownerAlias), convoName)
    }

    private fun toConvoObj(owner: ClientUser, convoName: String): Conversation {
        val processedConvoName = usersManager.replaceAliasesOccurrences(convoName, ClientUsersManager.FindBy.NAME_ALIAS)
        val backend = BackendConnections.get(owner)
        return backend.getConversationByName(owner, processedConvoName)
    }

    private fun useNewTestService() {
        isOldTestService = false
    }

    private fun deleteCustomBackend(customDomain: String) {
        BackendConnections.get("staging").deleteCustomBackendDomain(customDomain);
    }
}
