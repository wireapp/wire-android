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
import service.enums.LegalHoldStatus
import service.models.Conversation
import service.models.SendLocationParams
import service.models.SendTextParams
import user.usermanager.ClientUserManager
import user.utils.ClientUser
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.Duration
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

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
