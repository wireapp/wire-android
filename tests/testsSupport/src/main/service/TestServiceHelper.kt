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
import service.models.SendTextParams
import user.usermanager.ClientUserManager
import user.utils.ClientUser
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.Duration
import java.util.concurrent.TimeUnit

class TestServiceHelper {

    val WIRE_RECEIPT_MODE = "WIRE_RECEIPT_MODE"
    val usersManager by lazy {
        ClientUserManager.getInstance()
    }

    val testServiceClient by lazy {
        TestService("http://192.168.2.18:8080","TestService")
    }


    private  fun getRawResourceAsFile(context: Context, @RawRes rawResId: Int, fileName: String): File? {
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

    fun getSelfDeletingMessageTimeout(userAlias: String, conversationName: String): Duration {
        val user = usersManager.findUserByNameOrNameAlias(userAlias)

        // Only team users support enforced self-deleting messages
        user.teamId?.let {
            val settings = BackendClient.loadBackend(user.backendName.orEmpty()).getSelfDeletingMessagesSettings(user)

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

        // Personal user or team user without set enforced self-deleting message setting

        val resolvedConversationName = usersManager.replaceAliasesOccurrences(
            conversationName,
            ClientUserManager.FindBy.NAME_ALIAS
        )

        val messageTimerMillis =toConvoObjPersonal(user,resolvedConversationName).messageTimerInMilliseconds
        if (messageTimerMillis > 0) {
            return Duration.ofMillis(messageTimerMillis.toLong())
        }

        // Otherwise check for local/client-side self-deleting message timeout
        return Duration.ofSeconds(Long.MAX_VALUE)
    }

    fun contactSendsLocalAudioPersonalMLSConversation(
        context:Context,
        fileName: String,
        senderAlias: String,
        deviceName: String,
        dstConvoName: String
    ) {

        val audio = getRawResourceAsFile(context,R.raw.test, fileName)
        val  conversation = toConvoObjPersonal(senderAlias, dstConvoName)

        if(audio?.exists()!=true){
            throw Exception("Audio file not found")
        }

        val convoId = conversation.qualifiedID.id

        val convoDomain = conversation.qualifiedID.domain

        testServiceClient.sendFile(toClientUser(senderAlias), deviceName,
            convoId, convoDomain,
            getSelfDeletingMessageTimeout(senderAlias,dstConvoName),
            audio.absolutePath.orEmpty(),
            "audio/mp4")
    }

    fun contactSendsLocalTextPersonalMLSConversation(
        context:Context,
        fileName: String,
        senderAlias: String,
        deviceName: String,
        dstConvoName: String
    ) {

        val textFile = getRawResourceAsFile(context,R.raw.gistfile1, fileName)
        val  conversation = toConvoObjPersonal(senderAlias, dstConvoName)

        if(textFile?.exists()!=true){
            throw Exception("Text file not found")
        }

        val convoId = conversation.qualifiedID.id

        val convoDomain = conversation.qualifiedID.domain

        testServiceClient.sendFile(toClientUser(senderAlias), deviceName, convoId, convoDomain,
            getSelfDeletingMessageTimeout(senderAlias,dstConvoName), textFile.absolutePath.orEmpty(), "text/plain")

    }

    fun contactSendsLocalVideoPersonalMLSConversation(
        context:Context,
        fileName: String,
        senderAlias: String,
        deviceName: String,
        dstConvoName: String
    ) {

        val videoFile = getRawResourceAsFile(context,R.raw.testing, fileName)
        val  conversation = toConvoObjPersonal(senderAlias, dstConvoName)

        if(videoFile?.exists()!=true){
            throw Exception("Video file not found")
        }

        val convoId = conversation.qualifiedID.id

        val convoDomain = conversation.qualifiedID.domain

        testServiceClient.sendFile(toClientUser(senderAlias), deviceName, convoId, convoDomain,
            getSelfDeletingMessageTimeout(senderAlias,dstConvoName), videoFile?.absolutePath.orEmpty(),
            "video/mp4")

    }

    fun contactSendsLocalImagePersonalMLSConversation(
        context:Context,
        fileName: String,
        senderAlias: String,
        deviceName: String,
        dstConvoName: String
    ) {

        val imageFile = getRawResourceAsFile(context,R.raw.testing, fileName)
        val  conversation = toConvoObjPersonal(senderAlias, dstConvoName)

        if(imageFile?.exists()!=true){
            throw Exception("Video file not found")
        }

        val convoId = conversation.qualifiedID.id

        val convoDomain = conversation.qualifiedID.domain

        testServiceClient.sendFile(toClientUser(senderAlias), deviceName, convoId, convoDomain,
            getSelfDeletingMessageTimeout(senderAlias,dstConvoName), imageFile.absolutePath.orEmpty(),
            "image/jpeg")

    }



    private fun toConvoObjPersonal( ownerAlias:String,  convoName:String) : Conversation{
        return toConvoObjPersonal(toClientUser(ownerAlias), convoName)
    }

    private fun toConvoObjPersonal( owner:ClientUser,  convoName:String):Conversation {
        val convoName = usersManager.replaceAliasesOccurrences(convoName, ClientUserManager.FindBy.NAME_ALIAS);
        val backend = BackendClient.loadBackend(owner.backendName.orEmpty())
        return backend.getPersonalConversationByName(owner, convoName)
    }

    private fun toConvoObj(owner:ClientUser,  convoName:String):Conversation {
        val convoName = usersManager.replaceAliasesOccurrences(convoName, ClientUserManager.FindBy.NAME_ALIAS);
        val backend = BackendClient.loadBackend(owner.backendName.orEmpty())
        return backend.getConversationByName(owner, convoName)
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
    ) {
        val developmentApiEnabled = BackendClient.loadBackend(toClientUser(ownerAlias).backendName.orEmpty()).isDevelopmentApiEnabled(toClientUser(ownerAlias))
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

    fun isSendReadReceiptEnabled(userNameAlias: String): Boolean {
        val user = toClientUser(userNameAlias)
        val backend = BackendClient.loadBackend(user.backendName.orEmpty())
        val json = runBlocking {
            backend.getPropertyValues(user) }

        return if (json.has(WIRE_RECEIPT_MODE)) {
            json.getInt(WIRE_RECEIPT_MODE).toBoolean()
        } else {
            false
        }
    }
   private fun Int.toBoolean() : Boolean{
        return this != 0
    }

    fun userSendMessageToConversation(senderAlias:String, msg:String,
                                        deviceName:String,
                                      dstConvoName:String,
                                      isSelfDeleting:Boolean){
        val clientUser = toClientUser(senderAlias)
        val conversation = toConvoObj(clientUser, dstConvoName)
        val convoId = conversation.qualifiedID.id
        val convoDomain = conversation.qualifiedID.domain

        val expReadConfirm = conversation.type.let { type ->
            when (type) {
                0 -> conversation.isReceiptModeEnabled
                2 -> isSendReadReceiptEnabled(senderAlias)
                else -> false
            }
        }

         testServiceClient.sendText(
            SendTextParams(
                owner = clientUser,
            deviceName =  deviceName,
            convoDomain= convoDomain,
            convoId= convoId,
            timeout = if(isSelfDeleting) Duration.ofSeconds(1000) else Duration.ofSeconds(0),
            expReadConfirm,
            text = msg,
            legalHoldStatus = LegalHoldStatus.DISABLED.code,
        ))
    }

    fun toClientUser(nameAlias: String): ClientUser {
        return usersManager.findUserByNameOrNameAlias(nameAlias)
    }
}
