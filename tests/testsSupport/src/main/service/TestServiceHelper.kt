@file:Suppress("TooManyFunctions", "TooGenericExceptionCaught", "TooGenericExceptionThrown", "ReturnCount", "MagicNumber")
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

import android.content.Context
import androidx.annotation.RawRes
import backendUtils.BackendClient
import backendUtils.team.getTeamByName
import com.wire.android.testSupport.R
import com.wire.android.testSupport.service.TestService
import kotlinx.coroutines.runBlocking
import network.HttpRequestException
import org.json.JSONArray
import org.json.JSONObject
import service.enums.LegalHoldStatus
import service.enums.TeamService
import service.models.Conversation
import service.models.SendLocationParams
import service.models.SendTextParams
import uiautomatorutils.UiWaitUtils
import user.usermanager.ClientUserManager
import user.utils.ClientUser
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.Duration
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

@Suppress("LargeClass")
class TestServiceHelper(
    private val usersManager: ClientUserManager
) {
    val wireReceiptMode = "WIRE_RECEIPT_MODE"

    val testServiceClient by lazy {
        TestService("http://192.168.2.18:8080", "TestService")
    }

    private fun getRawResourceAsFile(context: Context, @RawRes rawResId: Int, fileName: String): File? {
        val cacheDir = context.cacheDir
        val outputFile = File(cacheDir, fileName)

        try {
            context.resources.openRawResource(rawResId).use { inputStream ->
                FileOutputStream(outputFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            return outputFile
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    private fun backendFor(user: ClientUser): BackendClient {
        val backendName = user.backendName
        return if (backendName.isNullOrBlank()) {
            BackendClient.getDefault()
                ?: throw IllegalStateException("No default backend configured for user '${user.name}'.")
        } else {
            BackendClient.loadBackend(backendName)
        }
    }

    fun getSelfDeletingMessageTimeout(userAlias: String, conversationName: String): Duration {
        val user = usersManager.findUserByNameOrNameAlias(userAlias)

        // Only team users support enforced self-deleting messages
        user.teamId?.let {
            val settings = backendFor(user).getSelfDeletingMessagesSettings(user)

            if (settings.getString("status") == "enabled") {
                val timeoutInSeconds = settings
                    .getJSONObject("config")
                    .getInt("enforcedTimeoutSeconds")

                if (timeoutInSeconds != 0) {
                    // Timeout value is enforced in team settings
                    return Duration.ofSeconds(timeoutInSeconds.toLong())
                }
            } else {
                // Timeout is disabled
                return Duration.ZERO
            }
        }

        // Personal user or team user without enforced setting
        val resolvedConversationName = usersManager.replaceAliasesOccurrences(
            conversationName,
            ClientUserManager.FindBy.NAME_ALIAS
        )

        val conversationMessageTimerMillis = getConversationMessageTimer(user, resolvedConversationName)
        if (conversationMessageTimerMillis > 0) {
            return Duration.ofMillis(conversationMessageTimerMillis.toLong())
        }

        return Duration.ofMillis(Int.MAX_VALUE.toLong()) // ~24.8 days, safe int millis
    }

    private fun getConversationMessageTimer(user: ClientUser, conversationName: String): Int {
        val isPersonalConversationName = runCatching {
            // If this succeeds, conversationName is a user name/alias (1:1 style like "user4Name")
            usersManager.findUserByNameOrNameAlias(conversationName)
        }.isSuccess

        val conversation = if (isPersonalConversationName) {
            // Personal first, fallback to group just in case
            runCatching { toConvoObjPersonal(user, conversationName) }
                .recoverCatching { toConvoObj(user, conversationName) }
                .getOrThrow()
        } else {
            // Group first, fallback to personal just in case
            runCatching { toConvoObj(user, conversationName) }
                .recoverCatching { toConvoObjPersonal(user, conversationName) }
                .getOrThrow()
        }

        return conversation.messageTimerInMilliseconds
    }

    fun contactSendsLocalAudioPersonalMLSConversation(
        context: Context,
        fileName: String,
        senderAlias: String,
        deviceName: String,
        dstConvoName: String
    ) {

        val audio = getRawResourceAsFile(context, R.raw.test, fileName)
        val conversation = toConvoObjPersonal(senderAlias, dstConvoName)

        if (audio?.exists() != true) {
            throw Exception("Audio file not found")
        }

        val convoId = conversation.qualifiedID.id

        val convoDomain = conversation.qualifiedID.domain

        testServiceClient.sendFile(
            toClientUser(senderAlias),
            deviceName,
            convoId,
            convoDomain,
            getSelfDeletingMessageTimeout(senderAlias, dstConvoName),
            audio.absolutePath.orEmpty(),
            "audio/mp4"
        )
    }

    fun contactSendsLocalAudioConversation(
        context: Context,
        fileName: String,
        senderAlias: String,
        deviceName: String,
        dstConvoName: String
    ) {
        val audio = getRawResourceAsFile(context, R.raw.test, fileName)
        val conversation = toConvoObj(toClientUser(senderAlias), dstConvoName)

            if (audio?.exists() != true) {
            throw Exception("Audio file not found")
        }

        val convoId = conversation.qualifiedID.id
        val convoDomain = conversation.qualifiedID.domain

        testServiceClient.sendFile(
            toClientUser(senderAlias),
            deviceName,
            convoId,
            convoDomain,
            getSelfDeletingMessageTimeout(senderAlias, dstConvoName),
            audio.absolutePath.orEmpty(),
            "audio/mp4"
        )
    }

    fun contactSendsLocalTextConversation(
        context: Context,
        fileName: String,
        senderAlias: String,
        deviceName: String,
        dstConvoName: String
    ) {
        val textFile = getRawResourceAsFile(context, R.raw.testing_text, fileName)
        val conversation = toConvoObj(toClientUser(senderAlias), dstConvoName)

        if (textFile?.exists() != true) {
            throw Exception("Text file not found")
        }

        testServiceClient.sendFile(
            toClientUser(senderAlias),
            deviceName,
            conversation.qualifiedID.id,
            conversation.qualifiedID.domain,
            getSelfDeletingMessageTimeout(senderAlias, dstConvoName),
            textFile.absolutePath.orEmpty(),
            "text/plain"
        )
    }

    fun contactSendsLocalVideoConversation(
        context: Context,
        fileName: String,
        senderAlias: String,
        deviceName: String,
        dstConvoName: String
    ) {
        val videoFile = getRawResourceAsFile(context, R.raw.testing, fileName)
        val conversation = toConvoObj(toClientUser(senderAlias), dstConvoName)

        if (videoFile?.exists() != true) {
            throw Exception("Video file not found")
        }

        testServiceClient.sendFile(
            toClientUser(senderAlias),
            deviceName,
            conversation.qualifiedID.id,
            conversation.qualifiedID.domain,
            getSelfDeletingMessageTimeout(senderAlias, dstConvoName),
            videoFile.absolutePath.orEmpty(),
            "video/mp4"
        )
    }

    fun contactSendsLocalImageConversation(
        context: Context,
        fileName: String,
        senderAlias: String,
        deviceName: String,
        dstConvoName: String
    ) {
        val imageFile = getRawResourceAsFile(context, R.raw.testing, fileName)
        val conversation = toConvoObj(toClientUser(senderAlias), dstConvoName)

        if (imageFile?.exists() != true) {
            throw Exception("Image file not found")
        }

        testServiceClient.sendFile(
            toClientUser(senderAlias),
            deviceName,
            conversation.qualifiedID.id,
            conversation.qualifiedID.domain,
            getSelfDeletingMessageTimeout(senderAlias, dstConvoName),
            imageFile.absolutePath.orEmpty(),
            "image/jpeg"
        )
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

    fun contactSendsLocalTextPersonalMLSConversation(
        context: Context,
        fileName: String,
        senderAlias: String,
        deviceName: String,
        dstConvoName: String
    ) {

        val textFile = getRawResourceAsFile(context, R.raw.testing_text, fileName)
        val conversation = toConvoObjPersonal(senderAlias, dstConvoName)

        if (textFile?.exists() != true) {
            throw Exception("Text file not found")
        }

        val convoId = conversation.qualifiedID.id

        val convoDomain = conversation.qualifiedID.domain

        testServiceClient.sendFile(
            toClientUser(senderAlias),
            deviceName,
            convoId,
            convoDomain,
            getSelfDeletingMessageTimeout(senderAlias, dstConvoName),
            textFile.absolutePath.orEmpty(),
            "text/plain"
        )
    }

    fun contactSendsLocalVideoPersonalMLSConversation(
        context: Context,
        fileName: String,
        senderAlias: String,
        deviceName: String,
        dstConvoName: String
    ) {

        val videoFile = getRawResourceAsFile(context, R.raw.testing, fileName)
        val conversation = toConvoObjPersonal(senderAlias, dstConvoName)

        if (videoFile?.exists() != true) {
            throw Exception("Video file not found")
        }

        val convoId = conversation.qualifiedID.id

        val convoDomain = conversation.qualifiedID.domain

        testServiceClient.sendFile(
            toClientUser(senderAlias),
            deviceName,
            convoId,
            convoDomain,
            getSelfDeletingMessageTimeout(senderAlias, dstConvoName),
            videoFile.absolutePath.orEmpty(),
            "video/mp4"
        )
    }

    fun contactSendsLocalImagePersonalMLSConversation(
        context: Context,
        fileName: String,
        senderAlias: String,
        deviceName: String,
        dstConvoName: String
    ) {

        val imageFile = getRawResourceAsFile(context, R.raw.testing, fileName)
        val conversation = toConvoObjPersonal(senderAlias, dstConvoName)

        if (imageFile?.exists() != true) {
            throw Exception("Video file not found")
        }

        val convoId = conversation.qualifiedID.id

        val convoDomain = conversation.qualifiedID.domain

        testServiceClient.sendFile(
            toClientUser(senderAlias),
            deviceName,
            convoId,
            convoDomain,
            getSelfDeletingMessageTimeout(senderAlias, dstConvoName),
            imageFile.absolutePath.orEmpty(),
            "image/jpeg"
        )
    }

    fun toConvoObjPersonal(ownerAlias: String, convoName: String): Conversation {
        return toConvoObjPersonal(toClientUser(ownerAlias), convoName)
    }

    fun toConvoObjPersonal(owner: ClientUser, convoName: String): Conversation {
        val seekName = usersManager.findUserByNameOrNameAlias(convoName).name
            ?: throw NoSuchElementException("User '$convoName' does not have a resolvable display name.")
        val backend = backendFor(owner)
        return backend.getPersonalConversationByName(owner, seekName)
    }

    fun toConvoObj(owner: ClientUser, convoName: String): Conversation {
        val convoName = usersManager.replaceAliasesOccurrences(convoName, ClientUserManager.FindBy.NAME_ALIAS)
        val backend = backendFor(owner)
        return backend.getConversationByName(owner, convoName)
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

    // region Bots

    fun userAddsBotToConversation(userWhoAddsAlias: String, botToAdd: String, chatName: String) {
        val userWhoAdds = toClientUser(userWhoAddsAlias)
        val backend = backendFor(userWhoAdds)
        val conversation = toConvoObj(userWhoAdds, chatName)
        runBlocking {
            backend.addServiceToConversation(userWhoAdds, botToAdd, conversation)
        }
    }

    // endregion Bots

    fun addDevice(
        ownerAlias: String,
        verificationCode: String? = null,
        deviceName: String? = null,
    ) {
        val owner = toClientUser(ownerAlias)
        val developmentApiEnabled = backendFor(owner).isDevelopmentApiEnabled(owner)
        try {
            testServiceClient.login(
                owner,
                verificationCode,
                deviceName,
                developmentApiEnabled
            )
        } catch (_: HttpRequestException) {
            try {
                TimeUnit.SECONDS.sleep(300)
                testServiceClient.login(
                    owner,
                    verificationCode,
                    deviceName,
                    developmentApiEnabled
                )
            } catch (_: Exception) {
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

        val backend = backendFor(chatOwner)

        runBlocking {
            val dstTeam = backend.getTeamByName(chatOwner, teamName)
            backend.createTeamConversation(chatOwner, participants, chatName, dstTeam)
        }
    }

    fun userCreatesInviteDeeplink(userNameAlias: String, conversationName: String): String {
        val user = toClientUser(userNameAlias)
        val backend = backendFor(user)
        val conversation = toConvoObj(user, conversationName)
        val inviteLink = backend.createInviteLink(user, conversation)
        return inviteLink.toConversationJoinDeepLink(backend.domain)
    }

    fun userCreatesInviteDeeplinkWithPassword(userNameAlias: String, conversationName: String, password: String): String {
        val user = toClientUser(userNameAlias)
        val backend = backendFor(user)
        val conversation = toConvoObj(user, conversationName)
        val inviteLink = backend.createInviteLinkWithPassword(user, conversation, password)
        return inviteLink.toConversationJoinDeepLink(backend.domain)
    }

    private fun String.toConversationJoinDeepLink(domain: String): String {
        val query = substringAfter("?", missingDelimiterValue = "")
        if (query.isBlank()) {
            throw IllegalStateException("Invite link has unexpected format: $this")
        }
        return "wire://conversation-join?$query&domain=$domain"
    }

    fun userGetsInviteDeeplink(userNameAlias: String, conversationName: String): String {
        val user = toClientUser(userNameAlias)
        val backend = backendFor(user)
        val conversation = toConvoObj(user, conversationName)
        val inviteLink = backend.getInviteLink(user, conversation)
        return inviteLink.toConversationJoinDeepLink(backend.domain)
    }

    fun userRevokesInviteLink(userNameAlias: String, conversationName: String) {
        val user = toClientUser(userNameAlias)
        val backend = backendFor(user)
        backend.revokeInviteLink(user, toConvoObj(user, conversationName))
    }

    fun userDeletesTeamConversation(userNameAlias: String, conversationName: String) {
        val user = toClientUser(userNameAlias)
        val backend = backendFor(user)
        backend.deleteTeamConversation(user, toConvoObj(user, conversationName))
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

        runBlocking {
            val dstTeam = backend.getTeamByName(chatOwner, teamName)
            backend.createTeamConversation(chatOwner, participants, null, dstTeam)
        }
    }

    fun isSendReadReceiptEnabled(userNameAlias: String): Boolean {
        val user = toClientUser(userNameAlias)
        val backend = backendFor(user)
        val json = runBlocking {
            backend.getPropertyValues(user)
        }

        return if (json.has(wireReceiptMode)) {
            json.getInt(wireReceiptMode).toBoolean()
        } else {
            false
        }
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

    private fun Int.toBoolean(): Boolean {
        return this != 0
    }

    @Suppress("LongParameterList")
    fun thereIsATeamOwner(
        context: Context,
        ownerNameAlias: String,
        teamName: String,
        updateHandle: Boolean,
        locale: String = "en_US",
        backend: BackendClient = BackendClient.getDefault()!!
    ) {
        val owner = toClientUser(ownerNameAlias)
        if (usersManager.isUserCreated(owner)) {
            throw Exception(
                "Cannot create team with user ${owner.nameAliases} as owner because user is already created"
            )
        }
        usersManager.createTeamOwnerByAlias(ownerNameAlias, teamName, locale, updateHandle, backend, context)
    }

    fun syncUserIdsForUsersCreatedThroughIdP(ownerNameAlias: String, user: ClientUser) {
        user.getUserIdThroughOwner = Callable {
            val asUser = toClientUser(ownerNameAlias)
            val backend = backendFor(asUser)
            val teamMembers = backend.getTeamMembers(asUser)

            for (member in teamMembers) {
                val memberId = member.userId

                val memberName = backend.getUserNameByID(backend.domain, memberId, asUser)

                if (user.name == memberName) {
                    return@Callable memberId
                }
            }

            throw IOException(
                "No user ID found for user ${user.email}. Please verify you are using the right Team Owner account"
            )
        }
    }

    fun toClientUser(nameAlias: String): ClientUser {
        return usersManager.findUserByNameOrNameAlias(nameAlias)
    }

    fun userSendMessageToConversation(
        senderAlias: String,
        msg: String,
        deviceName: String,
        dstConvoName: String,
        isSelfDeleting: Boolean
    ) {
        val clientUser = toClientUser(senderAlias)
        val conversation = toConvoObj(clientUser, dstConvoName)
        sendMessageInternal(
            clientUser = clientUser,
            conversation = conversation,
            msg = msg,
            deviceName = deviceName,
            timeout = resolveMessageTimeout(senderAlias, dstConvoName, isSelfDeleting)
        )
    }

    @Suppress("LongParameterList")
    fun userSendsPollMessageToConversation(
        senderAlias: String,
        msg: String,
        title: String?,
        buttonsCsv: String,
        deviceName: String?,
        dstConvoName: String
    ): String {
        val clientUser = toClientUser(senderAlias)
        val conversation = toConvoObj(clientUser, dstConvoName)
        val message = title?.let { "**$it**${System.lineSeparator()}$msg" } ?: msg
        val expReadConfirm = when (conversation.type) {
            0 -> conversation.isReceiptModeEnabled
            2 -> isSendReadReceiptEnabled(clientUser.name.orEmpty())
            else -> false
        }

        return testServiceClient.sendCompositeText(
            SendTextParams(
                owner = clientUser,
                deviceName = deviceName,
                convoDomain = conversation.qualifiedID.domain,
                convoId = conversation.qualifiedID.id,
                timeout = Duration.ZERO,
                expectsReadConfirmation = expReadConfirm,
                text = message,
                buttons = buttonsCsv.toButtonsJsonArray(),
                legalHoldStatus = LegalHoldStatus.DISABLED.code,
            )
        )
    }

    fun userSendsButtonActionConfirmationToLatestPollMessage(
        senderAlias: String,
        receiverAlias: String,
        deviceName: String?,
        dstConvoName: String,
        buttonText: String
    ) {
        val sender = toClientUser(senderAlias)
        val receiver = toClientUser(receiverAlias)
        val conversation = toConvoObj(sender, dstConvoName)
        val convoId = conversation.qualifiedID.id
        val convoDomain = conversation.qualifiedID.domain
        val pollMessage = getRecentPollMessage(sender, deviceName, convoId, convoDomain)
        val buttonId = pollMessage.pollButtonIdByText(buttonText)
        val receiverId = receiver.id ?: throw IllegalStateException("User '$receiverAlias' does not have an id")

        testServiceClient.sendButtonActionConfirmation(
            sender,
            receiverId,
            deviceName,
            convoId,
            pollMessage.getString("id"),
            buttonId
        )
    }

    fun userSendMessageToPersonalMlsConversation(
        senderAlias: String,
        msg: String,
        deviceName: String,
        dstConvoName: String,
        isSelfDeleting: Boolean
    ) {
        val clientUser = toClientUser(senderAlias)
        val conversation = toConvoObjPersonal(clientUser, dstConvoName)
        sendMessageInternal(
            clientUser = clientUser,
            conversation = conversation,
            msg = msg,
            deviceName = deviceName,
            timeout = resolveMessageTimeout(senderAlias, dstConvoName, isSelfDeleting)
        )
    }

    fun userTogglesReactionOnLatestMessage(
        senderAlias: String,
        convoName: String,
        deviceName: String,
        reaction: String
    ) {
        val sender = toClientUser(senderAlias)
        val conversation = toConvoObj(sender, convoName)
        val convoId = conversation.qualifiedID.id
        val convoDomain = conversation.qualifiedID.domain
        val recentMessageId = getRecentMessageId(sender, deviceName, convoId, convoDomain)

        testServiceClient.toggleReaction(
            sender,
            deviceName,
            convoId,
            convoDomain,
            recentMessageId,
            reaction
        )
    }

    private fun getRecentMessageId(
        user: ClientUser,
        deviceName: String?,
        convoId: String,
        convoDomain: String
    ): String {
        var messageIds = emptyList<String>()
        val hasMessages = UiWaitUtils.retryUntilTimeout(
            timeout = UiWaitUtils.MEDIUM_TIMEOUT,
            pollingInterval = 1.seconds
        ) {
            messageIds = testServiceClient.getMessageIds(user, deviceName, convoId, convoDomain)
            messageIds.isNotEmpty()
        }

        if (hasMessages) {
            return messageIds.last()
        }
        throw IllegalStateException("The conversation contains no messages")
    }

    private fun getRecentPollMessage(
        user: ClientUser,
        deviceName: String?,
        convoId: String,
        convoDomain: String
    ): JSONObject {
        var messages = JSONArray()
        val hasPollMessage = UiWaitUtils.retryUntilTimeout(
            timeout = UiWaitUtils.MEDIUM_TIMEOUT,
            pollingInterval = 1.seconds
        ) {
            messages = testServiceClient.getMessages(user, deviceName, convoId, convoDomain)
            (messages.length() - 1 downTo 0).any { index ->
                messages.getJSONObject(index).hasPollButtons()
            }
        }

        if (hasPollMessage) {
            for (index in messages.length() - 1 downTo 0) {
                val message = messages.getJSONObject(index)
                if (message.hasPollButtons()) return message
            }
        }
        throw IllegalStateException("Could not find poll message in conversation '$convoId'")
    }

    private fun JSONObject.hasPollButtons(): Boolean {
        val content = optJSONObject("content") ?: return false
        if (content.has("buttonList")) return true
        val items = content.optJSONArray("items") ?: return false
        return (0 until items.length()).any { index ->
            items.getJSONObject(index).has("button")
        }
    }

    private fun JSONObject.pollButtonIdByText(buttonText: String): String {
        val content = getJSONObject("content")
        content.optJSONArray("buttonList")?.let { buttonItems ->
            for (index in 0 until buttonItems.length()) {
                val buttonItem = buttonItems.getJSONObject(index)
                if (buttonItem.getString("text") == buttonText) return buttonItem.getString("id")
            }
        }
        content.optJSONArray("items")?.let { items ->
            for (index in 0 until items.length()) {
                val button = items.getJSONObject(index).optJSONObject("button") ?: continue
                if (button.getString("text") == buttonText) return button.getString("id")
            }
        }
        throw IllegalStateException("Expected poll button '$buttonText' not found")
    }

    private fun String.toButtonsJsonArray(): JSONArray =
        split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .fold(JSONArray()) { buttons, buttonText ->
                buttons.put(buttonText)
                buttons
            }

    private fun resolveMessageTimeout(
        senderAlias: String,
        dstConvoName: String,
        isSelfDeleting: Boolean
    ): Duration {
        return if (isSelfDeleting) {
            Duration.ofSeconds(1000)
        } else {
            getSelfDeletingMessageTimeout(senderAlias, dstConvoName).let { timeout ->
                if (timeout == Duration.ofMillis(Int.MAX_VALUE.toLong())) Duration.ZERO else timeout
            }
        }
    }

    fun userXSharesLocationTo(
        senderAlias: String,
        convoName: String,
        deviceName: String,
        isSelfDeleting: Boolean
    ) {
        val clientUser = toClientUser(senderAlias)
        val conversation = toConvoObj(clientUser, convoName)
        testServiceClient.sendLocation(
            SendLocationParams(
                owner = clientUser,
                deviceName = deviceName,
                convoId = conversation.id,
                convoDomain = conversation.qualifiedID.domain,
                timeout = if (isSelfDeleting) Duration.ofSeconds(1000) else Duration.ZERO,
                longitude = 0f,
                latitude = 0f,
                locationName = "location",
                zoom = 1
            )
        )
    }

    private fun sendMessageInternal(
        clientUser: ClientUser,
        conversation: Conversation,
        msg: String,
        deviceName: String,
        timeout: Duration
    ) {
        val convoId = conversation.qualifiedID.id
        val convoDomain = conversation.qualifiedID.domain

        val expReadConfirm = when (conversation.type) {
            0 -> conversation.isReceiptModeEnabled
            2 -> isSendReadReceiptEnabled(clientUser.name.orEmpty())
            else -> false
        }

        try {
            testServiceClient.sendText(
                SendTextParams(
                    owner = clientUser,
                    deviceName = deviceName,
                    convoDomain = convoDomain,
                    convoId = convoId,
                    timeout = timeout,
                    expectsReadConfirmation = expReadConfirm,
                    text = msg,
                    legalHoldStatus = LegalHoldStatus.DISABLED.code,
                )
            )
        } catch (e: Throwable) {
            throw AssertionError(
                "Failed to send message '$msg' to conversationId='$convoId' for user='${clientUser.name}' on device '$deviceName'.",
                e
            )
        }
    }
}
