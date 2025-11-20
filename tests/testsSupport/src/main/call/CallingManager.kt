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
package call

import android.graphics.Bitmap
import user.usermanager.ClientUserManager

import android.util.Base64
import android.util.Log
import call.models.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import android.graphics.BitmapFactory
import backendUtils.BackendClient
import backendUtils.team.getTeamByName
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.collection.IsEmptyIterable.emptyIterable
import org.json.JSONObject
import service.getConversationByName
import user.utils.ClientUser
import java.util.concurrent.TimeoutException

class CallingManager(private val usersManager: ClientUserManager) {
    companion object {
        private const val POLL_INTERVAL_MS = 2000L
        private const val TIMEOUT_INSTANCE_START = 190_000L
        private const val TIMEOUT_INSTANCE_DESTROY = 30_000L
        private const val TIMEOUT_POSITIVE_FLOWCHECK = 30_000L
        private const val TIMEOUT_NEGATIVE_FLOWCHECK = 20_000L
        private const val TIMEOUT_PEER_CONNECTIONS = 20_000L
        private const val FLOWCHECK_POLLING_MS = 2000L

        private const val ZCALL_SUPPORT_VERSION = "8.2.29"
        private const val ZCALL_CURRENT_VERSION = "9.8.15"
        private const val FIREFOX_SUPPORT_VERSION = "91.8.0esr"
        private const val FIREFOX_CURRENT_VERSION = "104.0"
        private const val CHROME_SUPPORT_VERSION = "102.0.5005.115"
        private const val CHROME_CURRENT_VERSION = "103.0.5060.53"
    }

    private val client = CallingServiceClient()
    private val instances = ConcurrentHashMap<String, Instance>()
    private val calls = ConcurrentHashMap<String, Call>()

    // region === Instance Management ===


    suspend fun startInstances(
        calleeNames: List<String>,
        instanceType: String,
        platform: String,
        scenarioName: String
    ) {
        calleeNames.map { calleeName ->
            CoroutineScope(Dispatchers.IO).async {
                startInstance(calleeName, "", instanceType, platform, scenarioName)
            }
        }.awaitAll()
    }

    suspend fun startInstance(
        name: String,
        verificationCode: String,
        instanceType: String,
        platform: String,
        scenarioName: String
    ) = withContext(Dispatchers.IO) {
        Log.d("CallingManager", "Creating instance for $name")

        val user = usersManager.findUserByNameOrNameAlias(name)
        BackendClient.loadBackend(user.backendName.orEmpty()).disableConsentPopup(user)

        val type = parseInstanceType(instanceType)
        val instance = client.startInstance(
            user,
            verificationCode,
            type,
            "$platform: $scenarioName",
            beta = true
        )

        instances[user.email.orEmpty()] = instance
        Log.d("CallingManager", "Started instance ${instance.id} for user ${user.name}")
        instance
    }

    suspend fun stopInstance(user: ClientUser) {
        val instance = instances[user.email.orEmpty()] ?: return
        withContext(Dispatchers.IO) {
            runCatching { client.stopInstance(instance) }
                .onFailure {
                    Log.w("CallingManager", "Failed to stop instance for ${user.name}: ${it.message}")
                }
        }
        instances.remove(user.email.orEmpty())
    }

    fun getInstance(user: ClientUser): Instance =
        instances[user.email.orEmpty()]
            ?: error("No instance for user '${user.name}', call startInstance first")


    suspend fun callConversation(callerName: String, conversationName: String) {
        val user = usersManager.findUserByNameOrNameAlias(callerName)
        val convId = getConversationId(user, conversationName)
        val call = withContext(Dispatchers.IO) {
            client.callToUser(getInstance(user), convId)
        }
        calls["${user.email}:$convId"] = call
    }

    suspend fun acceptNextCall(calleeNames: List<String>) {
        calleeNames.forEach { name ->
            val user = usersManager.findUserByNameOrNameAlias(name)
            val call = withContext(Dispatchers.IO) {
                client.acceptNextIncomingCall(getInstance(user))
            }
            calls[user.email.orEmpty()] = call
        }
    }

    suspend fun stopIncomingCall(calleeNames: List<String>) {
        calleeNames.forEach { name ->
            val user = usersManager.findUserByNameOrNameAlias(name)
            withContext(Dispatchers.IO) {
                client.stopCall(getInstance(user), getIncomingCall(user))
            }
        }
    }

    private fun getIncomingCall(user: ClientUser): Call =
        calls[user.email.orEmpty()] ?: error("No incoming call for user '${user.name}'")

    private fun getOutgoingCall(user: ClientUser, convId: String): Call =
        calls["${user.email.orEmpty()}:$convId"]
            ?: error("No outgoing call for ${user.name.orEmpty()} to conversation $convId")

    // endregion

    // region === Verifications ===

    suspend fun verifyInstanceStatus(
        userNames: List<String>,
        expected: InstanceStatus,
        timeoutMs: Long = TIMEOUT_INSTANCE_START
    ) = withContext(Dispatchers.IO) {
        userNames.forEach { name ->
            val user = usersManager.findUserByNameOrNameAlias(name)
            val instance = getInstance(user)
            withTimeout(timeoutMs) {
                while (true) {
                    val status = client.getInstanceStatus(instance)
                    if (status == expected) return@withTimeout
                    delay(POLL_INTERVAL_MS)
                }
            }
        }
    }

    suspend fun verifyCallStatus(
        callerName: String,
        conversationName: String,
        expected: List<CallStatus>,
        timeoutMs: Long = TIMEOUT_PEER_CONNECTIONS
    ) = withContext(Dispatchers.IO) {
        val user = usersManager.findUserByNameOrNameAlias(callerName)
        val convId = getConversationId(user, conversationName)
        val instance = getInstance(user)
        val call = getOutgoingCall(user, convId)

        withTimeout(timeoutMs) {
            while (true) {
                val status = client.getCall(instance, call).status
                if (status in expected) return@withTimeout
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    suspend fun verifyAcceptingCallStatus(
        calleeNames: List<String>,
        expectedStatuses: String,
        secondsTimeout: Int
    ) {
        for (calleeName in calleeNames) {
            val userAs = usersManager.findUserByNameOrNameAlias(calleeName)
            waitForExpectedCallStatuses(
                getInstance(userAs),
                getIncomingCall(userAs),
                callStatusesListToObject(expectedStatuses),
                secondsTimeout
            )
        }
    }

    private fun callStatusesListToObject(expectedStatuses: String): List<CallStatus> {
        val result = mutableListOf<CallStatus>()
        val allStatuses = expectedStatuses.split(",")

        for (status in allStatuses) {
            var clearedStatus = status.trim().uppercase()
            if (clearedStatus == "READY") {
                clearedStatus = "DESTROYED"
                // READY could mean DESTROYED or NON_EXISTENT so we add both
                result.add(CallStatus.NON_EXISTENT)
                println("WARNING: Please use DESTROYED or NON_EXISTENT instead of READY to check the state of a call! READY will be removed in future versions.")
            }
            result.add(CallStatus.valueOf(clearedStatus))
        }

        return result
    }


    private suspend fun waitForExpectedCallStatuses(
        instance: Instance,
        call: Call,
        expectedStatuses: List<CallStatus>,
        secondsTimeout: Int
    ) {
        val millisecondsStarted = System.currentTimeMillis()
        var currentStatus: CallStatus? = null

        while (System.currentTimeMillis() - millisecondsStarted <= secondsTimeout * 1000L) {
            currentStatus = client.getCall(instance, call).status
            if (expectedStatuses.contains(currentStatus)) {
                return
            }
            delay(2000)
        }

        throw TimeoutException(
            "Call status '$currentStatus' for instance '${instance.id}' " +
                    "has not been changed to '$expectedStatuses' after $secondsTimeout second(s) timeout"
        )
    }


    suspend fun <T> withTimeout(
        timeoutMs: Long,
        block: suspend CoroutineScope.() -> T
    ): T = coroutineScope {
        val result = CompletableDeferred<T>()

        // Launch the block in a new coroutine
        val job = launch {
            try {
                result.complete(block())
            } catch (e: Throwable) {
                result.completeExceptionally(e)
            }
        }

        // Wait for completion or timeout
        select<T> {
            result.onAwait { value ->
                job.cancel() // cancel timeout watcher if finished
                value // ✅ return the value here
            }

            onTimeout(timeoutMs) {
                job.cancel()
                throw Exception("Timed out after $timeoutMs ms") // ✅ return a T by throwing
            }
        }
    }

    // endregion

    // region === Utilities ===

    private fun parseInstanceType(value: String): VersionedInstanceType {
        val lower = value.lowercase()
        return if (lower.contains(":")) {
            val (type, version) = lower.split(":")
            val resolvedVersion = when (version) {
                "support" -> getSupportVersion(type)
                else -> version
            }
            VersionedInstanceType(type, resolvedVersion)
        } else {
            when (lower) {
                "chrome" -> VersionedInstanceType(lower, CHROME_CURRENT_VERSION)
                "firefox" -> VersionedInstanceType(lower, FIREFOX_CURRENT_VERSION)
                "zcall", "zcall_v3" -> VersionedInstanceType(lower, ZCALL_CURRENT_VERSION)
                else -> error("Unknown instance type: $value")
            }
        }
    }

    private fun getSupportVersion(type: String): String = when (type) {
        "chrome" -> CHROME_SUPPORT_VERSION
        "firefox" -> FIREFOX_SUPPORT_VERSION
        "zcall", "zcall_v3" -> ZCALL_SUPPORT_VERSION
        else -> error("Unknown instance type")
    }

    private fun getConversationId(owner: ClientUser, name: String): String {
        val backend: BackendClient = BackendClient.loadBackend(owner.backendName.orEmpty())
        return try {
            runBlocking {
                backend.getTeamByName(owner, name).id
            }
        } catch (_: Exception) {
            val convUser = usersManager.findUserByNameOrNameAlias(name)
            backend.getConversationByName(owner, convUser).id
        }
    }

    // region === Flow & Packet Verification ===

    suspend fun verifyPeerConnections(userNames: List<String>, expectedFlowCount: Int) {
        userNames.forEach { name ->
            val user = usersManager.findUserByNameOrNameAlias(name)

            waitUntil(
                timeoutMs = TIMEOUT_PEER_CONNECTIONS,
                intervalMs = FLOWCHECK_POLLING_MS
            ) {
                val flows = safeGetFlows(user).filter { !it.isEmptyFlow() }
                flows.size == expectedFlowCount
            }

            delay(3000)
            val finalFlows = safeGetFlows(user).filter { !it.isEmptyFlow() }
            check(finalFlows.size == expectedFlowCount) {
                "Unexpected number of flows for $name: got ${finalFlows.size}, expected $expectedFlowCount"
            }
        }
    }

    suspend fun verifyCbrConnections(userNames: List<String>) {
        userNames.forEach { name ->
            val user = usersManager.findUserByNameOrNameAlias(name)
            val packets = client.getPackets(getInstance(user))
            val json = runCatching { JSONObject(packets) }.getOrNull() ?: return@forEach

            val src = json.optString("src", "")
            val dst = json.optString("dst", "")

            fun frequencyMap(data: String) =
                data.split(" ").filter { it.isNotBlank() }
                    .groupingBy { it }.eachCount()

            val srcMap = frequencyMap(src)
            val dstMap = frequencyMap(dst)

            srcMap.forEach { (len, freq) ->
                Log.d("CallingManager", "SRC packets $len appeared $freq times for $name")
            }
            dstMap.forEach { (len, freq) ->
                Log.d("CallingManager", "DST packets $len appeared $freq times for $name")
            }

            check(srcMap.size == 1) { "RTP packets sourced from $name were not constant" }
            check(dstMap.size == 1) { "RTP packets destined to $name were not constant" }
        }
    }

    suspend fun verifySendAndReceiveAudio(userNames: List<String>) {
        userNames.forEach { name ->
            val user = usersManager.findUserByNameOrNameAlias(name)
            val flowsBefore = safeGetFlows(user)
            check(flowsBefore.isNotEmpty()) { "No flows found for $name" }

            for (flowBefore in flowsBefore)
                assertPositiveFlowChange(user, flowBefore, audioSent = true, audioRecv = true)
        }
    }


    suspend fun verifySendAndReceiveAudio(callees: String) {
        for (callee in usersManager.splitAliases(callees)) {
            val userAs = usersManager.findUserByNameOrNameAlias(callee);
            val flowsBefore = getFlows(callee)
            assertThat("Found no flows for $callee", flowsBefore, not(emptyIterable()))
            for (flowBefore in flowsBefore) {
                assertPositiveFlowChange(userAs,flowBefore,true, true, false, false)
            }
        }
    }


    suspend fun verifyReceiveAudio(userNames: List<String>) {
        userNames.forEach { name ->
            val user = usersManager.findUserByNameOrNameAlias(name)
            val flowsBefore = safeGetFlows(user)
            check(flowsBefore.isEmpty()) { "No flows found for $name" }

            for (flowBefore in flowsBefore)
                assertPositiveFlowChange(user, flowBefore, audioRecv = true)
        }
    }

    suspend fun verifySendAndReceiveAudioAndVideo(userNames: List<String>) {
        userNames.forEach { name ->
            val user = usersManager.findUserByNameOrNameAlias(name)
            val flowsBefore = safeGetFlows(user)
            check(flowsBefore.isEmpty()) { "No flows found for $name" }

            for (flowBefore in flowsBefore)
                assertPositiveFlowChange(user, flowBefore, audioSent = true, audioRecv = true, videoSent = true, videoRecv = true)
        }
    }

    suspend fun verifySendVideo(userNames: List<String>) {
        userNames.forEach { name ->
            val user = usersManager.findUserByNameOrNameAlias(name)
            val flowsBefore = safeGetFlows(user)
            check(flowsBefore.isEmpty()) { "No flows found for $name" }

            for (flowBefore in flowsBefore)
                assertPositiveFlowChange(user, flowBefore, videoSent = true)
        }
    }

    suspend fun verifyNotSendVideo(userNames: List<String>) {
        userNames.forEach { name ->
            val user = usersManager.findUserByNameOrNameAlias(name)
            val flowsBefore = safeGetFlows(user)
            for (flowBefore in flowsBefore)
                assertNoFlowChange(user, flowBefore, videoSent = true)
        }
    }

    suspend fun verifyReceiveAudioAndVideo(userNames: List<String>) {
        userNames.forEach { name ->
            val user = usersManager.findUserByNameOrNameAlias(name)
            val flowsBefore = safeGetFlows(user)
            for (flowBefore in flowsBefore)
                assertPositiveFlowChange(user, flowBefore, audioRecv = true, videoRecv = true)
        }
    }

    suspend fun verifyNotSendOrReceiveVideo(userNames: List<String>) {
        userNames.forEach { name ->
            val user = usersManager.findUserByNameOrNameAlias(name)
            val flowsBefore = safeGetFlows(user)
            for (flowBefore in flowsBefore)
                assertNoFlowChange(user, flowBefore, videoSent = true, videoRecv = true)
        }
    }

    private suspend fun assertPositiveFlowChange(
        user: ClientUser,
        flowBefore: CallFlow,
        audioSent: Boolean = false,
        audioRecv: Boolean = false,
        videoSent: Boolean = false,
        videoRecv: Boolean = false
    ) {
        val flows = mutableListOf<CallFlow>()

        val success = waitUntil(TIMEOUT_POSITIVE_FLOWCHECK, FLOWCHECK_POLLING_MS) {
            val flowSnapshot = getFlowForRemoteUser(user, flowBefore.remoteUserId)
            flows += flowSnapshot

            (!audioSent || flowSnapshot.audioPacketsSent > flowBefore.audioPacketsSent) &&
                    (!audioRecv || flowSnapshot.audioPacketsReceived > flowBefore.audioPacketsReceived) &&
                    (!videoSent || flowSnapshot.videoPacketsSent > flowBefore.videoPacketsSent) &&
                    (!videoRecv || flowSnapshot.videoPacketsReceived > flowBefore.videoPacketsReceived)
        }

        if (!success) {
            val flowAfter = flows.lastOrNull()
            error(
                "No change in packets for ${user.name}. After: ${flowAfter?.toPrettyString()} " +
                        "Before: ${flowBefore.toPrettyString()}"
            )
        }
    }

    private suspend fun assertNoFlowChange(
        user: ClientUser,
        flowBefore: CallFlow,
        audioSent: Boolean = false,
        audioRecv: Boolean = false,
        videoSent: Boolean = false,
        videoRecv: Boolean = false
    ) {
        val flows = mutableListOf<CallFlow>()

        val success = waitUntil(TIMEOUT_NEGATIVE_FLOWCHECK, FLOWCHECK_POLLING_MS) {
            val flowSnapshot = getFlowForRemoteUser(user, flowBefore.remoteUserId)
            flows += flowSnapshot

            (audioSent && flowSnapshot.audioPacketsSent > flowBefore.audioPacketsSent) ||
                    (audioRecv && flowSnapshot.audioPacketsReceived > flowBefore.audioPacketsReceived) ||
                    (videoSent && flowSnapshot.videoPacketsSent > flowBefore.videoPacketsSent) ||
                    (videoRecv && flowSnapshot.videoPacketsReceived > flowBefore.videoPacketsReceived)
        }

        if (success) {
            val flowAfter = flows.lastOrNull()
            error(
                "Unexpected flow change for ${user.name}. After: ${flowAfter?.toPrettyString()} " +
                        "Before: ${flowBefore.toPrettyString()}"
            )
        }
    }

    private suspend fun getFlowForRemoteUser(user: ClientUser, remoteUserId: String): CallFlow {
        val flows = safeGetFlows(user)
        return flows.firstOrNull { it.remoteUserId == remoteUserId }
            ?: error("Remote user $remoteUserId has no flows for ${user.name}")
    }

    private suspend fun safeGetFlows(user: ClientUser): List<CallFlow> =
        withContext(Dispatchers.IO) {
            val flows = client.getFlows(getInstance(user)).filter { !it.isEmptyFlow() }
            flows.ifEmpty {
                delay(2000)
                client.getFlows(getInstance(user))
            }
        }
    private suspend fun getFlows(user: String): List<CallFlow> =
        withContext(Dispatchers.IO) {
            val userAs = usersManager.findUserByNameOrNameAlias(user);
            val flows = client.getFlows(getInstance(userAs)).filter { !it.isEmptyFlow() }
            flows.ifEmpty {
                delay(2000)
                client.getFlows(getInstance(userAs))
            }
        }

    private fun CallFlow.isEmptyFlow() = audioPacketsSent == -1L && audioPacketsReceived == -1L

    private suspend fun waitUntil(
        timeoutMs: Long,
        intervalMs: Long,
        condition: suspend () -> Boolean
    ): Boolean {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (condition()) return true
            delay(intervalMs)
        }
        return false
    }

    // endregion

    // region === Screenshot & Live Preview ===

    suspend fun getLivePreview(user: ClientUser): Bitmap? = withContext(Dispatchers.IO) {
        val instance = getInstance(user)
        val dataUrl = client.getLivePreview(instance)
        val bytes = Base64.decode(dataUrl, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    suspend fun getScreenshot(user: ClientUser): Bitmap? = withContext(Dispatchers.IO) {
        client.getScreenshot(getInstance(user))
    }

    // endregion

    // region === Cleanup & Moderation ===

    suspend fun cleanup() = withContext(Dispatchers.IO) {
        if (instances.isEmpty()) return@withContext

        Log.d("CallingManager", "Cleaning up call instances...")

        instances.values.forEach { instance ->
            runCatching { client.stopInstance(instance) }
                .onFailure {
                    Log.w("CallingManager", "Failed to stop instance ${instance.id}: ${it.message}")
                }
        }

        instances.clear()
    }

    suspend fun switchVideoOn(userNames: List<String>) {
        userNames.forEach { name ->
            val user = usersManager.findUserByNameOrNameAlias(name)
            val instance = getInstance(user)
            client.getCurrentCall(instance)?.let { client.switchVideoOn(instance, it) }
            delay(500)
        }
    }

    suspend fun switchVideoOff(userNames: List<String>) {
        userNames.forEach { name ->
            val user = usersManager.findUserByNameOrNameAlias(name)
            val instance = getInstance(user)
            client.getCurrentCall(instance)?.let { client.switchVideoOff(instance, it) }
        }
    }

    suspend fun pauseVideoCall(userNames: List<String>) {
        userNames.forEach { name ->
            val user = usersManager.findUserByNameOrNameAlias(name)
            val instance = getInstance(user)
            client.getCurrentCall(instance)?.let { client.pauseVideoCall(instance, it) }
        }
    }

    suspend fun unpauseVideoCall(userNames: List<String>) {
        userNames.forEach { name ->
            val user = usersManager.findUserByNameOrNameAlias(name)
            val instance = getInstance(user)
            client.getCurrentCall(instance)?.let { client.unpauseVideoCall(instance, it) }
        }
    }

    suspend fun switchScreensharingOn(userNames: List<String>) {
        userNames.forEach { name ->
            val user = usersManager.findUserByNameOrNameAlias(name)
            val instance = getInstance(user)
            client.getCurrentCall(instance)?.let { client.switchScreensharingOn(instance, it) }
        }
    }

    suspend fun switchScreensharingOff(userNames: List<String>) {
        userNames.forEach { name ->
            val user = usersManager.findUserByNameOrNameAlias(name)
            val instance = getInstance(user)
            client.getCurrentCall(instance)?.let { client.switchScreensharingOff(instance, it) }
        }
    }

    suspend fun muteMicrophone(userNames: List<String>) {
        userNames.forEach { name ->
            val user = usersManager.findUserByNameOrNameAlias(name)
            val instance = getInstance(user)
            client.getCurrentCall(instance)?.let { client.muteMicrophone(instance, it) }
        }
    }

    suspend fun unmuteMicrophone(userNames: List<String>) {
        userNames.forEach { name ->
            val user = usersManager.findUserByNameOrNameAlias(name)
            val instance = getInstance(user)
            client.getCurrentCall(instance)?.let { client.unmuteMicrophone(instance, it) }
        }
    }


    suspend fun maximiseCall(userNames: List<String>) {
        userNames.forEach { name ->
            val user = usersManager.findUserByNameOrNameAlias(name)
            val instance = getInstance(user)
            client.getCurrentCall(instance)?.let { client.maximiseCall(instance, it) }
        }
    }

    suspend fun muteParticipant(moderatorName: String, participantName: String) {
        val moderator = usersManager.findUserByNameOrNameAlias(moderatorName)
        val instance = getInstance(moderator)
        client.getCurrentCall(instance)?.let { client.muteParticipant(instance, it, participantName) }
    }

    suspend fun muteAllOthers(moderatorName: String, participantName: String) {
        val moderator = usersManager.findUserByNameOrNameAlias(moderatorName)
        val instance = getInstance(moderator)
        client.getCurrentCall(instance)?.let { client.muteAllOthers(instance, it, participantName) }
    }

}


