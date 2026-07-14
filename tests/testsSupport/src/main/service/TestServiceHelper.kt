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
import android.graphics.Bitmap
import androidx.annotation.RawRes
import backendUtils.BackendClient
import backendUtils.conversation.getConversationByName
import backendUtils.conversation.getPersonalConversationByName
import backendUtils.team.getSelfDeletingMessagesSettings
import backendUtils.team.getTeamMembers
import backendUtils.user.getPropertyValues
import backendUtils.user.getUserNameByID
import backendUtils.user.isDevelopmentApiEnabled
import com.wire.android.testSupport.R
import com.wire.android.testSupport.service.TestService
import kotlinx.coroutines.runBlocking
import network.HttpRequestException
import service.enums.LegalHoldStatus
import service.models.Conversation
import service.models.SendFileParams
import service.models.SendLocationParams
import service.models.SendTextParams
import service.models.SendTextWithLinkParams
import uiautomatorutils.UiWaitUtils
import user.usermanager.ClientUserManager
import user.utils.ClientUser
import util.generateQRCode
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.time.Duration
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import java.util.regex.Matcher
import kotlin.time.Duration.Companion.seconds

class TestServiceHelper(
    private val usersManager: ClientUserManager
) {
    val wireReceiptMode = "WIRE_RECEIPT_MODE"
    private val noExpirationTimeout = Duration.ZERO

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

        return noExpirationTimeout
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

    fun contactSendsLocalImageConversation(
        context: Context,
        fileName: String,
        senderAlias: String,
        deviceName: String?,
        dstConvoName: String
    ) {
        val image = getRawResourceAsFile(context, R.raw.testing_image, fileName)
        val conversation = toConvoObj(toClientUser(senderAlias), dstConvoName)

        if (image?.exists() != true) {
            throw Exception("Image file not found")
        }

        testServiceClient.sendImage(
            SendFileParams(
                owner = toClientUser(senderAlias),
                deviceName = deviceName,
                convoId = conversation.qualifiedID.id,
                convoDomain = conversation.qualifiedID.domain,
                timeout = getSelfDeletingMessageTimeout(senderAlias, dstConvoName),
                filePath = image.absolutePath.orEmpty(),
                type = "image/jpeg",
                otherAlgorithm = false,
                otherHash = false,
                invalidHash = false
            )
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

    fun contactSendsOneMbTextFileConversation(
        context: Context,
        fileName: String,
        senderAlias: String,
        deviceName: String,
        dstConvoName: String
    ) {
        val textFile = File(context.cacheDir, fileName)
        RandomAccessFile(textFile, "rws").use { file ->
            file.setLength(1024L * 1024L)
        }
        val conversation = toConvoObj(toClientUser(senderAlias), dstConvoName)

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
        deviceName: String?,
        dstConvoName: String
    ) {
        val clientUser = toClientUser(senderAlias)
        val conversation = toConvoObj(clientUser, dstConvoName)
        sendMessageInternal(
            clientUser = clientUser,
            conversation = conversation,
            msg = msg,
            deviceName = deviceName,
            timeout = resolveMessageTimeout(senderAlias, dstConvoName)
        )
    }

    fun userSendMessageToPersonalMlsConversation(
        senderAlias: String,
        msg: String,
        deviceName: String?,
        dstConvoName: String
    ) {
        val clientUser = toClientUser(senderAlias)
        val conversation = toConvoObjPersonal(clientUser, dstConvoName)
        sendMessageInternal(
            clientUser = clientUser,
            conversation = conversation,
            msg = msg,
            deviceName = deviceName,
            timeout = resolveMessageTimeout(senderAlias, dstConvoName)
        )
    }

    fun userSendEphemeralMessageToConversation(
        senderAlias: String,
        msg: String,
        deviceName: String,
        dstConvoName: String,
        messageTimer: Duration
    ) {
        val clientUser = toClientUser(senderAlias)
        val conversation = if (runCatching { usersManager.findUserByNameOrNameAlias(dstConvoName) }.isSuccess) {
            toConvoObjPersonal(clientUser, dstConvoName)
        } else {
            toConvoObj(clientUser, dstConvoName)
        }
        sendMessageInternal(
            clientUser = clientUser,
            conversation = conversation,
            msg = msg,
            deviceName = deviceName,
            timeout = messageTimer
        )
    }

    fun userSendsGenericMessageToConversation(
        senderAlias: String,
        convoName: String,
        deviceName: String? = null,
        message: String
    ) {
        matchUrl(message)?.let { matcher ->
            val title = matcher.group(0).orEmpty()
            val bitmap = generateQRCode(title, 512)
            val tempFile = File.createTempFile("link_preview_", ".png")

            FileOutputStream(tempFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }

            userSendsLinkPreview(
                senderAlias = senderAlias,
                convoName = convoName,
                deviceName = deviceName,
                msg = message,
                title = title,
                imagePath = tempFile.absolutePath
            )

            tempFile.deleteOnExit()
        } ?: userSendMessageToConversation(senderAlias, message, deviceName.orEmpty(), convoName)
    }

    @Suppress("LongParameterList")
    private fun userSendsLinkPreview(
        senderAlias: String,
        convoName: String,
        deviceName: String? = null,
        msg: String,
        title: String,
        imagePath: String
    ) {
        val matcher = matchUrl(msg)
            ?: throw IllegalArgumentException("Text does not contain any URL: $msg")

        val urlOffset = matcher.regionStart()
        val url = matcher.group(0).orEmpty()

        val user = toClientUser(senderAlias)
        val conversation = toConvoObj(user, convoName)
        val messageTimer = resolveMessageTimeout(senderAlias, convoName)

        testServiceClient.sendLinkPreview(
            SendTextWithLinkParams(
                owner = user,
                deviceName = deviceName,
                convoId = conversation.qualifiedID.id,
                convoDomain = conversation.qualifiedID.domain,
                expectsReadConfirmation = false,
                text = msg,
                legalHoldStatus = LegalHoldStatus.ENABLED.code,
                summary = title,
                title = title,
                url = url,
                permUrl = url,
                urlOffset = urlOffset.toString(),
                filePath = imagePath,
                imagePath = imagePath,
                imageFile = File(imagePath),
                timeout = messageTimer,
                messageTimer = messageTimer,
            )
        )
    }

    private fun matchUrl(message: String): Matcher? {
        val pattern = Regex("""([a-z]+://)?[a-z0-9\-]+\.[a-z]+[^\s\n]*""", RegexOption.IGNORE_CASE)
        val matcher = pattern.toPattern().matcher(message)
        return if (matcher.find()) matcher else null
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

    private fun resolveMessageTimeout(
        senderAlias: String,
        dstConvoName: String
    ): Duration {
        return getSelfDeletingMessageTimeout(senderAlias, dstConvoName)
    }

    fun userXSharesLocationTo(
        senderAlias: String,
        convoName: String,
        deviceName: String
    ) {
        val clientUser = toClientUser(senderAlias)
        val conversation = toConvoObj(clientUser, convoName)
        val timeout = resolveMessageTimeout(senderAlias, convoName)

        testServiceClient.sendLocation(
            SendLocationParams(
                owner = clientUser,
                deviceName = deviceName,
                convoId = conversation.id,
                convoDomain = conversation.qualifiedID.domain,
                timeout = timeout,
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
        deviceName: String?,
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
