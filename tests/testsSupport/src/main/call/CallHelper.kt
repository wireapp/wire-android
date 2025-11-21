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
@file:Suppress(
    "TooGenericExceptionCaught",
    "LargeClass",
    "LongParameterList",
    "NestedBlockDepth",
    "MagicNumber",
    "TooManyFunctions",
    "TooGenericExceptionThrown"
)

package call

import backendUtils.BackendClient
import backendUtils.team.getTeamByName
import call.pinger.UiAutomatorPinger
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import user.usermanager.ClientUserManager

class CallHelper {
    lateinit var usersManager: ClientUserManager
    lateinit var callingManager: CallingManager

    fun init(usersManager: ClientUserManager) {
        this.usersManager = usersManager
        callingManager = CallingManager(usersManager)
    }

    suspend fun enableConferenceCallingFeatureViaBackdoorTeam(adminUserAlias: String, teamName: String) {
        if ((::usersManager).isInitialized.not()) {
            throw Exception("Users manager not intialized!")
        }
        val adminUser = usersManager.toClientUser(adminUserAlias)
        val backend = BackendClient.loadBackend(adminUser.backendName.orEmpty())
        val dstTeam = backend.getTeamByName(adminUser, teamName)
        backend.unlockConferenceCallingFeature(dstTeam)
        backend.enableConferenceCallingBackdoorViaBackendTeam(dstTeam)
    }

    suspend fun userXStartsInstance(callees: String, callingServiceBackend: String) {
        UiAutomatorPinger.startPinging()
        callingManager.startInstances(
            usersManager.splitAliases(callees),
            callingServiceBackend,
            "Android",
            "Testing"
        )
        UiAutomatorPinger.stopPinging()
    }

    suspend fun userXAcceptsNextIncomingCallAutomatically(callees: String) {
        callingManager.acceptNextCall(usersManager.splitAliases(callees))
    }

    suspend fun userVerifiesCallStatusToUserY(callees: String, expectedStatuses: String, timeoutSeconds: Int) {
        UiAutomatorPinger.startPinging()
        callingManager.verifyAcceptingCallStatus(
            usersManager.splitAliases(callees),
            expectedStatuses,
            timeoutSeconds
        )
        UiAutomatorPinger.stopPinging()
    }

    fun iSeeParticipantsInGroupCall(participants: String) {
        // 1. Resolve aliases into real usernames
        val groupMember = usersManager.replaceAliasesOccurrences(
            participants,
            ClientUserManager.FindBy.NAME_ALIAS
        )
        // 2. Split into individual names and check each one
        groupMember
            .split(",")
            .map { it.trim() }
            .forEach { participant ->
                try {
                    UiWaitUtils.waitElement(UiSelectorParams(text = participant))
                } catch (e: AssertionError) {
                    throw AssertionError("User '$participant' is not visible in the call", e)
                }
            }
    }

    suspend fun userVerifiesAudio(callees: String) {
        callingManager.verifySendAndReceiveAudio(callees)
    }
}
