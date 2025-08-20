@file:Suppress("TooManyFunctions", "LongParameterList", "LargeClass")
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
package com.wire.android.testSupport.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import backendUtils.BackendClient
import logger.WireTestLogger
import network.HttpRequestException
import org.json.JSONArray
import org.json.JSONObject
import service.HttpStatus
import service.enums.TypingStatus
import service.models.Mentions
import service.models.SendFileParams
import service.models.SendLocationParams
import service.models.SendTextParams
import service.models.SendTextWithLinkParams
import user.utils.ClientUser
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import javax.annotation.Nullable

class TestService(private val baseUri: String, private val testName: String) {

    companion object {
        private val log = WireTestLogger.getLog(TestService::class.java.simpleName)
        private const val CONNECT_TIMEOUT = 120000
        private const val STATUS_204 = 204
        private const val ZINFRA = "staging.zinfra.io"
        private const val STATUS_OK = 200
        private const val READ_TIMEOUT = 120000
        private const val LARGE_TEXT = 5000
        private const val SHORT_TEXT = 100
    }

    // userAliases => deviceName => instanceId
    private val userAliases = ConcurrentHashMap<String, MutableMap<String, String>>()

    // region HTTP connection logic
    private fun buildRequest(path: String, requestType: String): HttpURLConnection {
        val url = URL("$baseUri/$path")
        val c = url.openConnection() as HttpURLConnection
        c.requestMethod = requestType // PUT, POST, DELETE, GET
        c.doOutput = true
        c.connectTimeout = CONNECT_TIMEOUT
        c.readTimeout = READ_TIMEOUT
        c.setRequestProperty("Accept-Encoding", "UTF-8")
        c.setRequestProperty("Content-Type", "application/json")
        return c
    }

    private fun sendHttpRequest(c: HttpURLConnection, @Nullable request: JSONObject?): String {
        var response = ""
        var status = -1
        try {
            log.info("${c.requestMethod}: ${c.url}")
            request?.let {
                log.info(" >>> Request: ${truncateOnlyOnBig(it.toString())}")
                writeStream(it.toString(), c.outputStream)
            }
            status = c.responseCode
            response = readStream(c.inputStream)
            logResponseAndStatusCode(response, status)
            assertResponseCode(status, listOf(STATUS_OK, STATUS_204))
            return response
        } catch (e: IOException) {
            try {
                response = readStream(c.errorStream)
            } catch (ex: IOException) {
                log.fine("Could not read error stream: ${e.message}")
            }
            val error = "${e.message} ($status): $response"
            log.severe(error)
            throw HttpRequestException(error, status)
        } finally {
            c.disconnect()
        }
    }

    @Throws(IOException::class)
    private fun writeStream(data: String, os: OutputStream) {
        DataOutputStream(os).use { wr ->
            BufferedWriter(OutputStreamWriter(wr, StandardCharsets.UTF_8)).use { writer ->
                writer.write(data)
            }
        }
    }

    @Throws(IOException::class)
    private fun readStream(inputStream: InputStream?): String {
        if (inputStream != null) {
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                return reader.readLines().joinToString("")
            }
        }
        return ""
    }

    private fun logResponseAndStatusCode(response: String, responseCode: Int) {
        if (response.isEmpty()) {
            log.info(" >>> Response ($responseCode) with no response body")
        } else {
            if (log.isLoggable(Level.FINE)) {
                log.info(" >>> Response ($responseCode): ${truncateOnlyOnBig(response)}")
            } else {
                log.info(" >>> Response ($responseCode): $response")
            }
        }
    }

    private fun truncate(text: String) = truncate(text, SHORT_TEXT)

    private fun truncateOnlyOnBig(text: String) = truncate(text, LARGE_TEXT)

    private fun truncate(text: String, maxLength: Int): String {
        return if (text.length > maxLength) {
            text.substring(0, maxLength) + "..."
        } else {
            text
        }
    }

    private fun assertResponseCode(responseCode: Int, acceptableResponseCodes: List<Int>) {
        if (!acceptableResponseCodes.contains(responseCode)) {
            throw HttpRequestException(
                "Testservice request failed. Request return code is: $responseCode. Expected code is: ${
                    acceptableResponseCodes.joinToString(",")
                }.",
                responseCode
            )
        }
    }
    // endregion HTTP connection logic

    fun login(
        owner: ClientUser,
        verificationCode: String?,
        deviceName: String?,
        developmentApiEnabled: Boolean
    ) {
        val connection = buildRequest("api/v1/instance", "PUT")
        val requestBody = JSONObject().apply {
            put("email", owner.email)
            put("password", owner.password)
            verificationCode?.let { put("verificationCode", it) }
            put("deviceName", deviceName)
            put("name", testName)
            if (owner.backendName == "staging") {
                put("backend", "staging")
            } else {
                val ownerBackend = BackendClient.loadBackend(owner.backendName.orEmpty())
                val customBackend = JSONObject().apply {
                    put("name", ownerBackend.name)
                    put("rest", ownerBackend.backendUrl)
                    put("ws", ownerBackend.backendWebsocket)
                }
                put("customBackend", customBackend)
                put("federationDomain", ownerBackend.domain)
            }
            if (developmentApiEnabled && isKaliumTestservice()) {
                put("developmentApiEnabled", true)
            }
        }
        val result = sendHttpRequest(connection, requestBody)
        val responseBody = JSONObject(result)
        val instanceId = responseBody.getString("instanceId")
        userAliases.compute(owner.name.orEmpty()) { _, value ->
            if (value == null) {
                val devices = ConcurrentHashMap<String, String>()
                deviceName?.let { devices[it] = instanceId }
                devices
            } else {
                deviceName?.let { value[it] = instanceId }
                value
            }
        }
    }

    fun setAvailability(
        owner: ClientUser,
        deviceName: String?,
        teamId: String,
        availabilityType: Int
    ) {
        log.info("Availability status is only send to known devices. Make sure the user has send a message before.")
        val instanceId = getInstanceId(owner, deviceName)
        val connection = buildRequest("api/v1/instance/$instanceId/availability", "POST")
        val requestBody = JSONObject().apply {
            put("teamId", teamId)
            put("type", availabilityType)
        }
        sendHttpRequest(connection, requestBody)
    }

    @Suppress("LongParameterList")
    fun sendText(params: SendTextParams) = sendTextBase(params)

    @Suppress("LongParameterList")
    fun sendCompositeText(params: SendTextParams) = sendTextBase(params)

    @Suppress("LongParameterList")
    private fun sendTextBase(
        params: SendTextParams
    ): String {
        val instanceId = getInstanceId(params.owner, params.deviceName)
        val connection = buildRequest("api/v1/instance/$instanceId/sendText", "POST")
        val requestBody = JSONObject().apply {
            if (params.convoDomain != ZINFRA) {
                put("conversationDomain", params.convoDomain)
            }
            put("conversationId", params.convoId)
            if (params.timeout.toMillis() > 0) {
                put("messageTimer", params.timeout.toMillis())
            }
            if (params.expectsReadConfirmation) {
                put("expectsReadConfirmation", true)
            }
            put("text", params.text)
            put("buttons", params.buttons)
            put("legalHoldStatus", params.legalHoldStatus)
        }
        val result = sendHttpRequest(connection, requestBody)
        val response = JSONObject(result)
        return response.getString("messageId")
    }

    @Suppress("LongParameterList")
    fun updateText(
        owner: ClientUser,
        deviceName: String?,
        convoId: String,
        convoDomain: String,
        messageId: String,
        text: String
    ) {
        val instanceId = getInstanceId(owner, deviceName)
        val connection = buildRequest("api/v1/instance/$instanceId/updateText", "POST")
        val requestBody = JSONObject().apply {
            put("conversationId", convoId)
            if (convoDomain != ZINFRA) {
                put("conversationDomain", convoDomain)
            }
            put("firstMessageId", messageId)
            put("text", text)
        }
        sendHttpRequest(connection, requestBody)
    }

    @Suppress("TooGenericExceptionCaught, LongParameterList", "NestedBlockDepth")
    fun updateTextWithLinkPreview(
        params: SendTextWithLinkParams
    ) {
        val instanceId = getInstanceId(params.owner, params.deviceName)
        val connection = buildRequest("api/v1/instance/$instanceId/updateText", "POST")

        val requestBody = JSONObject().apply {
            put("conversationId", params.convoId)
            if (params.convoDomain != ZINFRA) {
                put("conversationDomain", params.convoDomain)
            }
            put("firstMessageId", params.messageId)
            put("text", params.text)

            val linkPreview = JSONObject().apply {
                if (params.filePath.isNotBlank()) {
                    val imageFile = File(params.filePath)
                    if (params.imageFile.exists()) {
                        val imageBytes = imageFile.readBytes()
                        val encodedBase64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

                        val bitmap = BitmapFactory.decodeFile(params.filePath)

                        val requestImage = JSONObject().apply {
                            put("data", encodedBase64)
                            put("width", bitmap?.width ?: 0)
                            put("height", bitmap?.height ?: 0)
                            put("type", "image/${params.filePath.substringAfterLast('.', "png")}")
                        }

                        put("image", requestImage)
                    }
                }

                put("summary", params.summary)
                put("title", params.title)
                put("url", params.url)
                put("urlOffset", params.urlOffset)
                put("permanentUrl", params.permUrl)
            }

            put("linkPreview", linkPreview)
        }

        sendHttpRequest(connection, requestBody)
    }

    fun sendTyping(
        owner: ClientUser,
        deviceName: String?,
        convoId: String,
        status: TypingStatus
    ) {
        val instanceId = getInstanceId(owner, deviceName)
        val connection = buildRequest("api/v1/instance/$instanceId/sendTyping", "POST")
        val requestBody = JSONObject().apply {
            put("conversationId", convoId)
            put("status", status.name.lowercase())
        }
        sendHttpRequest(connection, requestBody)
    }

    fun clearConversation(
        owner: ClientUser,
        deviceName: String?,
        convoId: String,
        convoDomain: String
    ) {
        val instanceId = getInstanceId(owner, deviceName)
        val connection = buildRequest("api/v1/instance/$instanceId/clear", "POST")
        val requestBody = JSONObject().apply {
            put("conversationId", convoId)
            if (convoDomain != ZINFRA) {
                put("conversationDomain", convoDomain)
            }
        }
        sendHttpRequest(connection, requestBody)
    }

    fun archiveConversation(
        owner: ClientUser,
        deviceName: String?,
        convoId: String
    ) {
        val instanceId = getInstanceId(owner, deviceName)
        val connection = buildRequest("api/v1/instance/$instanceId/archive", "POST")
        val requestBody = JSONObject().apply {
            put("archive", true)
            put("conversationId", convoId)
        }
        sendHttpRequest(connection, requestBody)
    }

    fun unarchiveConversation(
        owner: ClientUser,
        deviceName: String?,
        convoId: String
    ) {
        val instanceId = getInstanceId(owner, deviceName)
        val connection = buildRequest("api/v1/instance/$instanceId/archive", "POST")
        val requestBody = JSONObject().apply {
            put("archive", false)
            put("conversationId", convoId)
        }
        sendHttpRequest(connection, requestBody)
    }

    fun deleteForMe(
        owner: ClientUser,
        deviceName: String?,
        convoId: String,
        convoDomain: String,
        messageId: String
    ) {
        val instanceId = getInstanceId(owner, deviceName)
        val connection = buildRequest("api/v1/instance/$instanceId/delete", "POST")
        val requestBody = JSONObject().apply {
            put("conversationId", convoId)
            if (convoDomain != ZINFRA) {
                put("conversationDomain", convoDomain)
            }
            put("messageId", messageId)
        }
        sendHttpRequest(connection, requestBody)
    }

    fun deleteEverywhere(
        owner: ClientUser,
        deviceName: String?,
        convoId: String,
        convoDomain: String,
        messageId: String
    ) {
        val instanceId = getInstanceId(owner, deviceName)
        val connection = buildRequest("api/v1/instance/$instanceId/deleteEverywhere", "POST")
        val requestBody = JSONObject().apply {
            put("conversationId", convoId)
            if (convoDomain != ZINFRA) {
                put("conversationDomain", convoDomain)
            }
            put("messageId", messageId)
        }
        sendHttpRequest(connection, requestBody)
    }

    @Suppress("LongParameterList")
    fun sendFile(
        owner: ClientUser,
        deviceName: String?,
        convoId: String,
        convoDomain: String,
        timeout: Duration,
        filePath: String,
        type: String
    ) = sendFile(
        SendFileParams(
            owner = owner,
            deviceName = deviceName,
            convoDomain = convoDomain,
            convoId = convoId,
            timeout = timeout,
            filePath = filePath,
            type = type,
            otherHash = false,
            otherAlgorithm = false,
            invalidHash = false
        )
    )

    @Suppress("LongParameterList")
    fun sendFile(
        params: SendFileParams
    ) {
        val file = File(params.filePath)
        val instanceId = getInstanceId(params.owner, params.deviceName)
        val connection = buildRequest("api/v1/instance/$instanceId/sendFile", "POST")
        val requestBody = JSONObject().apply {
            put("conversationId", params.convoId)
            if (params.convoDomain != ZINFRA) {
                put("conversationDomain", params.convoDomain)
            }
            put("data", fileToBase64String(file))
            put("fileName", file.name)
            put("messageTimer", 0)
            put("type", params.type)
            put("otherAlgorithm", params.otherAlgorithm)
            put("otherHash", params.otherHash)
            put("invalidHash", params.invalidHash)
        }
        sendHttpRequest(connection, requestBody)
    }

    @Suppress("LongParameterList")
    fun sendAudioFile(
        params: SendFileParams
    ) {
        val file = File(params.filePath)
        val instanceId = getInstanceId(params.owner, params.deviceName)
        val connection = buildRequest("api/v1/instance/$instanceId/sendFile", "POST")
        val metadata = JSONObject().apply {
            put("durationInMillis", params.duration.toMillis().toInt())
            put("normalizedLoudness", params.normalizedLoudness)
        }
        val requestBody = JSONObject().apply {
            put("audio", metadata)
            put("conversationId", params.convoId)
            if (params.convoDomain != ZINFRA) {
                put("conversationDomain", params.convoDomain)
            }
            put("data", fileToBase64String(file))
            put("fileName", file.name)
            if (params.timeout.toMillis() > 0) {
                put("messageTimer", params.timeout.toMillis())
            }
            put("type", params.type)
        }
        sendHttpRequest(connection, requestBody)
    }

    @Suppress("LongParameterList")
    fun sendVideoFile(
        params: SendFileParams
    ) {
        val file = File(params.filePath)
        val instanceId = getInstanceId(params.owner, params.deviceName)
        val connection = buildRequest("api/v1/instance/$instanceId/sendFile", "POST")
        val metadata = JSONObject().apply {
            put("durationInMillis", params.duration.toMillis().toInt())
            put("height", params.dimensions[0])
            put("width", params.dimensions[1])
        }
        val requestBody = JSONObject().apply {
            put("video", metadata)
            put("conversationId", params.convoId)
            if (params.convoDomain != ZINFRA) {
                put("conversationDomain", params.convoDomain)
            }
            put("data", fileToBase64String(file))
            put("fileName", file.name)
            if (params.timeout.toMillis() > 0) {
                put("messageTimer", params.timeout.toMillis())
            }
            put("type", params.type)
        }
        sendHttpRequest(connection, requestBody)
    }

    @Suppress("LongParameterList")
    fun sendImage(
        params: SendFileParams
    ) {
        val inputStream = File(params.filePath).inputStream()
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        val instanceId = getInstanceId(params.owner, params.deviceName)
        val connection = buildRequest("api/v1/instance/$instanceId/sendImage", "POST")
        val requestBody = JSONObject().apply {
            put("conversationId", params.convoId)
            if (params.convoDomain != ZINFRA) {
                put("conversationDomain", params.convoDomain)
            }
            put("data", fileToBase64String(File(params.filePath)))
            put("width", bitmap.width)
            put("height", bitmap.height)
            if (params.timeout.toMillis() > 0) {
                put("messageTimer", params.timeout.toMillis())
            }
            put("type", "image/${params.filePath.substringAfterLast('.')}")
        }
        sendHttpRequest(connection, requestBody)
    }

    @Suppress("MagicNumber")
    fun sendImage(
        owner: ClientUser,
        deviceName: String?,
        convoId: String,
        convoDomain: String,
        timeout: Duration,
        bitmap: Bitmap,
        type: String
    ) {
        val byteArrayOutputStream = ByteArrayOutputStream()
        when (type.lowercase()) {
            "png" -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            "jpg", "jpeg" -> bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            else -> throw IllegalArgumentException("Unsupported image type: $type")
        }
        val imageBytes = byteArrayOutputStream.toByteArray()

        val instanceId = getInstanceId(owner, deviceName)
        val connection = buildRequest("api/v1/instance/$instanceId/sendImage", "POST")
        val requestBody = JSONObject().apply {
            put("conversationId", convoId)
            if (convoDomain != ZINFRA) {
                put("conversationDomain", convoDomain)
            }
            put("data", Base64.encodeToString(imageBytes, Base64.DEFAULT))
            put("width", bitmap.width)
            put("height", bitmap.height)
            if (timeout.toMillis() > 0) {
                put("messageTimer", timeout.toMillis())
            }
            put("type", "image/$type")
        }
        sendHttpRequest(connection, requestBody)
    }

    private fun fileToBase64String(srcFile: File): String {
        require(srcFile.exists()) {
            "The file at path '${srcFile.absolutePath}' is not accessible or does not exist"
        }
        val inputStream = FileInputStream(srcFile)
        val bytes = inputStream.readBytes()
        inputStream.close()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    @Suppress("TooGenericExceptionCaught, LongParameterList", "NestedBlockDepth")
    fun sendLinkPreview(
        params: SendTextWithLinkParams
    ) {
        val instanceId = getInstanceId(params.owner, params.deviceName)
        val connection = buildRequest("api/v1/instance/$instanceId/sendText", "POST")

        val requestBody = JSONObject().apply {
            put("conversationId", params.convoId)

            if (params.convoDomain != ZINFRA) {
                put("conversationDomain", params.convoDomain)
            }

            put("text", params.text)

            if (params.messageTimer.toMillis() > 0) {
                put("messageTimer", params.messageTimer.toMillis())
            }

            val linkPreview = JSONObject().apply {
                params.imagePath?.let {
                    val imageFile = File(it)
                    if (imageFile.exists()) {
                        val imageBytes = imageFile.readBytes()
                        val bitmap = BitmapFactory.decodeFile(it)

                        val requestImage = JSONObject().apply {
                            put("data", Base64.encodeToString(imageBytes, Base64.NO_WRAP))
                            put("width", bitmap?.width ?: 0)
                            put("height", bitmap?.height ?: 0)
                            put("type", "image/${it.substringAfterLast('.', "png")}")
                        }

                        put("image", requestImage)
                    }
                }

                put("summary", params.summary)
                put("title", params.title)
                put("url", params.url)
                put("urlOffset", params.urlOffset)
                put("permanentUrl", params.permUrl)
            }

            put("linkPreview", linkPreview)
        }

        sendHttpRequest(connection, requestBody)
    }

    fun sendTextWithMentions(
        params: SendTextParams
    ) = sendTextWithMentionsBase(params)

    fun sendCompositeTextWithMentions(
        params: SendTextParams
    ) = sendTextWithMentionsBase(params)

    private fun sendTextWithMentionsBase(
        params: SendTextParams
    ) {
        val instanceId = getInstanceId(params.owner, params.deviceName)
        val connection = buildRequest("api/v1/instance/$instanceId/sendText", "POST")
        val requestBody = buildRequestBody(
            params.convoId,
            params.convoDomain,
            params.text,
            params.buttons,
            params.messageTimer,
            params.listOfMentions
        )
        sendHttpRequest(connection, requestBody)
    }

    private fun buildRequestBody(
        convoId: String,
        convoDomain: String,
        text: String,
        buttons: JSONArray?,
        messageTimer: Duration,
        listOfMentions: List<Mentions>
    ): JSONObject {
        return JSONObject().apply {
            put("conversationId", convoId)
            addConversationDomainIfNotStaging(convoDomain)
            put("text", text)
            addButtonsIfPresent(buttons)
            addMessageTimerIfNeeded(messageTimer)
            put("mentions", buildMentionsJson(listOfMentions, convoDomain))
        }
    }

    private fun JSONObject.addConversationDomainIfNotStaging(convoDomain: String) {
        if (convoDomain != ZINFRA) {
            put("conversationDomain", convoDomain)
        }
    }

    private fun JSONObject.addButtonsIfPresent(buttons: JSONArray?) {
        buttons?.let { put("buttons", it) }
    }

    private fun JSONObject.addMessageTimerIfNeeded(messageTimer: Duration) {
        if (messageTimer.toMillis() > 0) {
            put("messageTimer", messageTimer.toMillis())
        }
    }

    private fun buildMentionsJson(listOfMentions: List<Mentions>, convoDomain: String): JSONArray {
        return JSONArray().apply {
            listOfMentions.forEach { mention ->
                put(buildMentionJson(mention, convoDomain))
            }
        }
    }

    private fun buildMentionJson(mention: Mentions, convoDomain: String): JSONObject {
        return JSONObject().apply {
            put("length", mention.length)
            put("start", mention.start)
            put("userId", mention.userId)
            if (convoDomain != ZINFRA) {
                put("userDomain", mention.userDomain)
            }
        }
    }

    fun sendReply(
        params: SendTextParams,
        hash: String
    ) {
        val instanceId = getInstanceId(params.owner, params.deviceName)
        val connection = buildRequest("api/v1/instance/$instanceId/sendText", "POST")
        val requestBody = JSONObject().apply {
            put(
                "conversationId",
                params.convoId
            )
            if (params.convoDomain != ZINFRA) {
                put("conversationDomain", params.convoDomain)
            }
            put("text", params.text)
            if (params.messageTimer.toMillis() > 0) {
                put("messageTimer", params.messageTimer.toMillis())
            }
            val quote = JSONObject().apply {
                put("quotedMessageId", params.messageId)
                put("quotedMessageSha256", hash)
            }
            put("quote", quote)
        }
        sendHttpRequest(connection, requestBody)
    }

    fun sendLocation(
        params: SendLocationParams
    ) {
        val instanceId = getInstanceId(params.owner, params.deviceName)
        val connection = buildRequest("api/v1/instance/$instanceId/sendLocation", "POST")
        val requestBody = JSONObject().apply {
            put("conversationId", params.convoId)
            if (params.convoDomain != ZINFRA) {
                put("conversationDomain", params.convoDomain)
            }
            put("latitude", params.latitude)
            put("locationName", params.locationName)
            put("longitude", params.longitude)
            if (params.timeout.toMillis() > 0) {
                put("messageTimer", params.timeout.toMillis())
            }
            put("zoom", params.zoom)
        }
        sendHttpRequest(connection, requestBody)
    }

    fun sendPing(
        owner: ClientUser,
        deviceName: String?,
        convoId: String,
        convoDomain: String,
        timeout: Duration
    ) {
        val instanceId = getInstanceId(owner, deviceName)
        val connection = buildRequest("api/v1/instance/$instanceId/sendPing", "POST")
        val requestBody = JSONObject().apply {
            put("conversationId", convoId)
            if (convoDomain != ZINFRA) {
                put("conversationDomain", convoDomain)
            }
            if (timeout.toMillis() > 0L) {
                put("messageTimer", timeout.toMillis())
            }
        }
        sendHttpRequest(connection, requestBody)
    }

    fun getDeviceId(user: ClientUser, deviceName: String): String {
        // caution: the returned device ids are not zero-padded which means they sometime lack a leading 0
        val instanceId = getInstanceId(user, deviceName)
        val connection = buildRequest("api/v1/instance/$instanceId", "GET")
        val response = sendHttpRequest(connection, null)
        val instance = JSONObject(response)
        return instance.getString("clientId")
    }

    fun getDeviceFingerprint(user: ClientUser, deviceName: String): String {
        val instanceId = getInstanceId(user, deviceName)
        val connection = buildRequest("api/v1/instance/$instanceId/fingerprint", "GET")
        val response = sendHttpRequest(connection, null)
        val instance = JSONObject(response)
        return instance.getString("fingerprint")
    }

    fun getMessages(
        owner: ClientUser,
        deviceName: String?,
        convoId: String,
        convoDomain: String
    ): JSONArray {
        val instanceId = getInstanceId(owner, deviceName)
        val connection = buildRequest("api/v1/instance/$instanceId/getMessages", "POST")
        val requestBody = JSONObject().apply {
            put("conversationId", convoId)
            if (convoDomain != ZINFRA) {
                put("conversationDomain", convoDomain)
            }
        }
        val response = sendHttpRequest(connection, requestBody)
        return JSONArray(response)
    }

    fun getMessageReadReceipts(
        owner: ClientUser,
        deviceName: String?,
        convoId: String,
        convoDomain: String,
        messageId: String
    ): JSONArray {
        val instanceId = getInstanceId(owner, deviceName)
        val connection = buildRequest("api/v1/instance/$instanceId/getMessageReadReceipts", "POST")
        val requestBody = JSONObject().apply {
            put("conversationId", convoId)
            if (convoDomain != ZINFRA) {
                put("conversationDomain", convoDomain)
            }
            put("messageId", messageId)
        }
        val response = sendHttpRequest(connection, requestBody)
        return JSONArray(response)
    }

    fun getMessageIds(
        user: ClientUser,
        deviceName: String?,
        convoId: String,
        convoDomain: String
    ): List<String> {
        val messages = getMessages(user, deviceName, convoId, convoDomain)
        val messageIds = mutableListOf<String>()
        for (i in 0 until messages.length()) {
            val message = messages.getJSONObject(i)
            messageIds.add(message.getString("id"))
        }
        return messageIds
    }

    fun getMessageIdByText(
        owner: ClientUser,
        deviceName: String?,
        convoId: String,
        convoDomain: String,
        text: String
    ): String {
        val messages: JSONArray = getMessages(owner, deviceName, convoId, convoDomain)
        for (i in 0 until messages.length()) {
            val message = messages.getJSONObject(i)
            val content = message.optJSONObject("content")
            if (content != null && content.optString("text") == text) {
                return message.getString("id")
            }
        }
        throw IllegalStateException("Could not find message with '$text' in messages on Testservice")
    }

    fun getMessage(
        owner: ClientUser,
        deviceName: String?,
        convoId: String,
        convoDomain: String,
        messageId: String
    ): JSONObject {
        val messages = getMessages(owner, deviceName, convoId, convoDomain)
        for (i in 0 until messages.length()) {
            val message = messages.getJSONObject(i)
            if (messageId == message.getString("id")) {
                return message
            }
        }
        throw IllegalStateException("Could not find message with id '$messageId' in messages on Testservice")
    }

    fun sendConfirmationDelivered(
        owner: ClientUser,
        deviceName: String?,
        convoId: String,
        convoDomain: String,
        messageId: String
    ) {
        val instanceId = getInstanceId(owner, deviceName)
        val connection = buildRequest("api/v1/instance/$instanceId/sendConfirmationDelivered", "POST")
        val requestBody = JSONObject().apply {
            put("conversationId", convoId)
            if (convoDomain != ZINFRA) {
                put("conversationDomain", convoDomain)
            }
            put("firstMessageId", messageId)
        }
        sendHttpRequest(connection, requestBody)
    }

    fun sendConfirmationRead(
        owner: ClientUser,
        deviceName: String?,
        convoId: String,
        convoDomain: String,
        messageId: String
    ) {
        val instanceId = getInstanceId(owner, deviceName)
        val connection = buildRequest("api/v1/instance/$instanceId/sendConfirmationRead", "POST")
        val requestBody = JSONObject().apply {
            put("conversationId", convoId)
            if (convoDomain != ZINFRA) {
                put("conversationDomain", convoDomain)
            }
            put("firstMessageId", messageId)
        }
        sendHttpRequest(connection, requestBody)
    }

    fun sendButtonAction(
        sender: ClientUser,
        receiverId: String,
        deviceName: String?,
        convoId: String,
        referenceMessageId: String,
        buttonId: String
    ) {
        val instanceId = getInstanceId(sender, deviceName)
        val connection = buildRequest("api/v1/instance/$instanceId/sendButtonAction", "POST")
        val requestBody = JSONObject().apply {
            put("buttonId", buttonId)
            put("referenceMessageId", referenceMessageId)
            put("userIds", arrayOf(receiverId))
            put("conversationId", convoId)
        }
        sendHttpRequest(connection, requestBody)
    }

    fun sendButtonActionConfirmation(
        owner: ClientUser,
        receiverId: String,
        deviceName: String?,
        convoId: String,
        referenceMessageId: String,
        buttonId: String
    ) {
        val instanceId = getInstanceId(owner, deviceName)
        val connection = buildRequest("api/v1/instance/$instanceId/sendButtonActionConfirmation", "POST")
        val requestBody = JSONObject().apply {
            put("buttonId", buttonId)
            put("referenceMessageId", referenceMessageId)
            put("userIds", arrayOf(receiverId))
            put("conversationId", convoId)
        }
        sendHttpRequest(connection, requestBody)
    }

    fun toggleReaction(
        owner: ClientUser,
        deviceName: String?,
        convoId: String,
        convoDomain: String,
        originalMessageId: String,
        reaction: String
    ) {
        val instanceId = getInstanceId(owner, deviceName)
        val connection = buildRequest("api/v1/instance/$instanceId/sendReaction", "POST")
        val requestBody = JSONObject().apply {
            put("conversationId", convoId)
            if (convoDomain != ZINFRA) {
                put("conversationDomain", convoDomain)
            }
            put("originalMessageId", originalMessageId)
            put("type", reaction)
        }
        sendHttpRequest(connection, requestBody)
    }

    fun sendEphemeralConfirmationDelivered(
        owner: ClientUser,
        deviceName: String?,
        convoId: String,
        convoDomain: String,
        messageId: String
    ) {
        val instanceId = getInstanceId(owner, deviceName)
        val connection = buildRequest("api/v1/instance/$instanceId/sendEphemeralConfirmationDelivered", "POST")
        val requestBody = JSONObject().apply {
            put("conversationId", convoId)
            if (convoDomain != ZINFRA) {
                put("conversationDomain", convoDomain)
            }
            put("firstMessageId", messageId)
        }
        sendHttpRequest(connection, requestBody)
    }

    fun breakSession(
        owner: ClientUser,
        deviceName: String,
        user: ClientUser,
        userDomain: String,
        deviceId: String
    ) {
        val instanceId = getInstanceId(owner, deviceName)
        val connection = buildRequest("api/v1/instance/$instanceId/breakSession", "POST")
        val requestBody = JSONObject().apply {
            put("userId", user.id)
            if (userDomain != ZINFRA) {
                put("userDomain", userDomain)
            }
            put("clientId", deviceId)
        }
        sendHttpRequest(connection, requestBody)
    }

    fun resetSession(
        owner: ClientUser,
        deviceName: String,
        convoId: String
    ) {
        val instanceId = getInstanceId(owner, deviceName)
        val connection = buildRequest("api/v1/instance/$instanceId/sendSessionReset", "POST")
        val requestBody = JSONObject().apply {
            put("conversationId", convoId)
        }
        sendHttpRequest(connection, requestBody)
    }

    fun createConversation(
        owner: ClientUser,
        participants: List<ClientUser>,
        chatName: String,
        deviceName: String
    ) {
        val instanceId = getInstanceId(owner, deviceName)
        val connection = buildRequest("api/v1/instance/$instanceId/conversation", "POST")
        val requestBody = JSONObject().apply {
            put("name", chatName)
            val userIds = participants.map { user ->
                "${user.id}@${BackendClient.loadBackend(user.backendName.orEmpty()).domain}"
            }
            put("userIds", JSONArray(userIds))
        }
        sendHttpRequest(connection, requestBody)
    }

    fun isKaliumTestservice(): Boolean {
        val c = buildRequest("api/v1/", "GET")
        var response = ""
        var status = -1
        try {
            log.info("${c.requestMethod}: ${c.url}")
            status = c.responseCode
            return (status == HttpStatus.NOT_FOUND.code)
        } catch (e: IOException) {
            try {
                response = readStream(c.errorStream)
            } catch (ex: IOException) {
                log.fine("Could not read error stream: ${e.message}")
            }
            val error = "${e.message} ($status): $response"
            log.severe(error)
            throw HttpRequestException(error, status)
        } finally {
            c.disconnect()
        }
    }

    fun cleanUp(owner: ClientUser, deviceName: String?) {
        val instanceId = getInstanceId(owner, deviceName)
        val connection = buildRequest("api/v1/instance/$instanceId", "DELETE")
        sendHttpRequest(connection, null)
    }

    fun cleanUp() {
        val allInstanceIds = mutableListOf<String>()
        userAliases.values.forEach { allInstanceIds.addAll(it.values) }
        userAliases.clear()

        for (instanceId in allInstanceIds) {
            val connection = buildRequest("api/v1/instance/$instanceId", "DELETE")
            sendHttpRequest(connection, null)
        }
    }

    fun getUserDevices(owner: ClientUser): List<String> {
        return ArrayList(userAliases.getOrDefault(owner.name, emptyMap()).keys.toList())
    }

    @Suppress("TooGenericExceptionThrown")
    private fun getInstanceId(user: ClientUser, deviceName: String?): String {
        log.info("Looking for device: $deviceName")

        var finalDeviceName = deviceName
        if (finalDeviceName == null) {
            val devices = userAliases[user.name]
            if (devices == null) {
                finalDeviceName = "Device1"
                log.info("No device found yet. Creating new one called $finalDeviceName")
                val backend = BackendClient.loadBackend(user.backendName.orEmpty())
                val developmentApiEnabled = backend.isDevelopmentApiEnabled(user)
                val verificationCode = if (backend.name.contains("bund")) {
                    backend.getVerificationCode(user)
                } else {
                    null
                }
                login(user, verificationCode, finalDeviceName, developmentApiEnabled)
            } else {
                finalDeviceName = devices.keys.first()
            }
        }

        val instanceId = userAliases
            .computeIfAbsent(user.name.orEmpty()) { ConcurrentHashMap() }[finalDeviceName]

        if (instanceId == null) {
            throw RuntimeException(
                "No pre-created device $finalDeviceName for user ${user.name} found. " +
                        "Maybe you forgot to add the step to explicitly add this device?"
            )
        }
        return instanceId
    }
}
