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
@file:Suppress("TooManyFunctions", "LongParameterList", "MagicNumber", "PackageNaming")

package backendUtils.team

import ImageUtil
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import backendUtils.BackendClient
import backendUtils.auth.defaultheaders
import backendUtils.auth.getAuthToken
import backendUtils.user.getActivationCodeForEmail
import com.wire.android.testSupport.R
import com.wire.android.testSupport.backendConnections.team.Team
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import logger.WireTestLogger
import network.HttpRequestException
import network.HttpResponseWithCookies
import network.NetworkBackendClient
import network.NumberSequence
import network.RequestOptions
import org.json.JSONArray
import org.json.JSONObject
import service.models.Conversation
import service.models.TeamMember
import user.utils.AccessCookie
import user.utils.AccessCredentials
import user.utils.AccessToken
import user.utils.Asset
import user.utils.ClientUser
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder

// Helper extensions
private fun jsonOf(vararg pairs: Pair<String, Any?>): JSONObject {
    return JSONObject().apply {
        pairs.forEach { (key, value) -> value?.let { put(key, it) } }
    }
}

private fun ClientUser.updateFromResponse(response: HttpResponseWithCookies, teamId: String? = null) {
    val json = JSONObject(response.body)
    id = json.getString("id")
    this.teamId = teamId ?: json.optString("team")
    accessCredentials = AccessCredentials(
        accessToken = accessCredentials?.accessToken,
        accessCookie = AccessCookie("zuid", response.cookies)
    )
}

private suspend fun BackendClient.registerUser(
    user: ClientUser,
    registrationBody: JSONObject,
    teamId: String? = null,
    updatePicture: Boolean = false,
    updateHandle: Boolean = false,
    context: Context? = null
): ClientUser {
    val response = sendTeamRequest(
        route = TeamRoutes.Register.route,
        method = "POST",
        body = registrationBody.toString()
    )
    user.updateFromResponse(response, teamId)
    if (updatePicture && context != null) updateUserPicture(user, context)
    if (updateHandle) updateUniqueUsername(user, user.uniqueUsername.orEmpty())
    return user
}

private fun BackendClient.sendTeamRequest(
    route: String,
    method: String,
    user: ClientUser? = null,
    body: String? = null,
    additionalHeaders: Map<String, String> = emptyMap()
): HttpResponseWithCookies {
    val headers = defaultheaders.toMutableMap().apply {
        putAll(additionalHeaders)
        user?.let {
            runBlocking {
                getAuthToken(it)?.let { token ->
                    put("Authorization", "Bearer ${token.value}")
                }
            }
        }
    }

    return NetworkBackendClient.sendJsonRequestWithCookies(
        url = URL(route.composeCompleteUrl()),
        method = method,
        body = body,
        headers = headers
    )
}

// Original functions refactored
private fun BackendClient.bookEmail(email: String): String {
    sendTeamRequest(
        route = TeamRoutes.BookEmail.route,
        method = "POST",
        body = jsonOf("email" to email).toString()
    )
    return "Sent activation email"
}

@Throws(NoSuchElementException::class)
suspend fun BackendClient.getTeamByName(forUser: ClientUser, teamName: String): Team {
    return getAllTeams(forUser)
        .firstOrNull { it.name.equals(teamName, ignoreCase = true) }
        ?: throw NoSuchElementException("Cannot find team with name '$teamName'")
}

@Throws(IOException::class)
suspend fun BackendClient.getAllTeams(forUser: ClientUser): List<Team> {
    val connection = NetworkBackendClient.makeRequest(
        url = URL("teams".composeCompleteUrl()),
        method = "GET",
        options = RequestOptions(accessToken = getAuthToken(forUser)),
        headers = defaultheaders
    )

    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
        throw IOException("Failed to fetch teams: ${connection.responseCode} ${connection.responseMessage}")
    }

    val responseJson = JSONObject(connection.inputStream.bufferedReader().readText())
    val teamsArray = responseJson.getJSONArray("teams")

    return List(teamsArray.length()) { i ->
        Team.fromJSON(teamsArray.getJSONObject(i))
    }
}

fun BackendClient.getTeamMembers(asUser: ClientUser): List<TeamMember> {
    val firstTeam = runBlocking { getAllTeams(asUser).first() }
    return getTeamMembers(runBlocking { getAuthToken(asUser)!! }, firstTeam.id)
}

private fun BackendClient.getTeamMembers(token: AccessToken, teamId: String): List<TeamMember> {
    val url = URL("teams/$teamId/members".composeCompleteUrl())

    val headers = defaultheaders.toMutableMap().apply {
        put("Authorization", "${token.type} ${token.value}")
        put("Accept", BackendClient.applicationJson)
    }

    val response = NetworkBackendClient.sendJsonRequestWithCookies(
        url = url,
        method = "GET",
        headers = headers,
        options = RequestOptions(
            accessToken = token,
            expectedResponseCodes = NumberSequence.Array(intArrayOf(HttpURLConnection.HTTP_OK))
        )
    )

    val jsonResponse = JSONObject(response.body)
    val members = jsonResponse.getJSONArray("members")

    return buildList {
        for (i in 0 until members.length()) {
            val member = members.getJSONObject(i)
            val userId = member.getString("user")
            val permissions = member.getJSONObject("permissions")
            val role = TeamRoles.getByPermissionBitMask(permissions.getInt("self"))
            add(TeamMember(userId, role))
        }
    }
}

suspend fun BackendClient.createTeamOwnerViaBackend(
    user: ClientUser,
    teamName: String,
    locale: String,
    updateHandle: Boolean,
    context: Context,
): ClientUser {
    bookEmail(user.email.orEmpty())
    val activationCode = getActivationCodeForEmail(user.email.orEmpty())

    val registrationBody = jsonOf(
        "email" to user.email,
        "name" to user.name,
        "locale" to locale,
        "password" to user.password,
        "email_code" to activationCode,
        "team" to jsonOf(
            "name" to teamName,
            "icon" to "default",
            "binding" to true
        )
    )
    return registerUser(user, registrationBody, updatePicture = true, updateHandle = updateHandle, context = context)
}

fun getImageInputStream(context: Context) = context.resources.openRawResource(R.drawable.default_team_avatar)

fun BackendClient.updateUserProfileImage(user: ClientUser, context: Context) {
    runBlocking {
        updateUserPicture(user, context)
    }
}

private suspend fun BackendClient.updateUserPicture(user: ClientUser, context: Context) {
    val bitmap = getImageInputStream(context).use {
        BitmapFactory.decodeStream(it)
    } ?: throw IllegalStateException("Couldn't get avatar")
    updateUserPicture(user, bitmap)
}

@Suppress("LongParameterList")
fun BackendClient.createTeamUserViaBackend(
    teamOwner: ClientUser,
    teamId: String,
    member: ClientUser,
    uploadPicture: Boolean,
    hasHandle: Boolean,
    role: TeamRoles,
    context: Context,
): ClientUser {
    runBlocking {
        val token = getAuthToken(teamOwner)
        val invitationId = inviteNewUserToTeam(
            token?.value.orEmpty(),
            teamId,
            member.email ?: "",
            teamOwner.name ?: "",
            role
        )

        val invitationCode = getTeamCode(teamId, invitationId)
        val registrationBody = jsonOf(
            "email" to member.email,
            "name" to member.name,
            "password" to member.password,
            "team_code" to invitationCode
        )

        registerUser(
            user = member,
            registrationBody = registrationBody,
            teamId = teamId,
            updatePicture = uploadPicture,
            updateHandle = hasHandle,
            context = context
        )
    }
    return member
}

fun BackendClient.acceptInvite(teamId: String, member: ClientUser): ClientUser {
    val invitationCode = getTeamCode(teamId, member.id.orEmpty())
    val registrationBody = jsonOf(
        "email" to member.email,
        "name" to member.name,
        "password" to member.password,
        "team_code" to invitationCode
    )
    return runBlocking {
        registerUser(
            user = member,
            registrationBody = registrationBody,
            teamId = teamId,
            updatePicture = false,
            updateHandle = false,
            context = null
        )
    }
}

fun BackendClient.getTeamId(user: ClientUser): String {
    val token = runBlocking { getAuthToken(user) }
    val url = "self".composeCompleteUrl()

    val headers = defaultheaders.toMutableMap().apply {
        put("Authorization", "${token?.type} ${token?.value}")
    }

    val response = NetworkBackendClient.sendJsonRequestWithCookies(
        url = URL(url),
        method = "GET",
        headers = headers,
        options = RequestOptions(accessToken = token)
    )

    return JSONObject(response.body).getString("team")
}

fun BackendClient.getTeamCode(teamId: String, invitationId: String): String {
    val encodedTeamId = URLEncoder.encode(teamId, "UTF-8")
    val encodedInvitationId = URLEncoder.encode(invitationId, "UTF-8")
    val response = sendTeamRequest(
        route = "i/teams/invitation-code?team=$encodedTeamId&invitation_id=$encodedInvitationId",
        method = "GET",
        additionalHeaders = mapOf("Authorization" to basicAuth.getEncoded())
    )
    return JSONObject(response.body).getString("code")
}

fun BackendClient.inviteNewUserToTeam(
    token: String,
    teamId: String,
    dstEmail: String,
    inviterName: String,
    role: TeamRoles
): String {
    val response = sendTeamRequest(
        route = "teams/$teamId/invitations",
        method = "POST",
        body = jsonOf(
            "email" to dstEmail,
            "role" to role.role,
            "inviter_name" to inviterName
        ).toString(),
        additionalHeaders = mapOf("Authorization" to "Bearer $token")
    )
    return JSONObject(response.body).getString("id")
}

private suspend fun BackendClient.uploadImageAsset(
    token: AccessToken?,
    imageBytes: ByteArray
): String {
    return retryOnBackendFailure {
        NetworkBackendClient.uploadAsset(
            URL(TeamRoutes.UploadAsset.route.composeCompleteUrl()),
            token,
            true,
            "eternal",
            imageBytes
        )
    }
}

private suspend fun BackendClient.updateUserPicture(user: ClientUser, image: Bitmap) {
    val token = getAuthToken(user)
    val square = ImageUtil.cropToSquare(image)
    val preview = ImageUtil.scaleTo(square, 200, 200)
    val previewKey = uploadImageAsset(token, ImageUtil.asByteArray(preview))
    val completeKey = uploadImageAsset(token, ImageUtil.asByteArray(image))

    val assets = setOf(
        Asset(previewKey, "image", "preview"),
        Asset(completeKey, "image", "complete")
    )
    retryOnBackendFailure {
        updateSelfAssets(token, assets)
        null
    }
}

private fun ClientUser.sendAuthenticatedRequest(
    backend: BackendClient,
    route: String,
    method: String,
    body: String? = null,
    expectedResponseCodes: NumberSequence = NumberSequence.Range(200..299),
    additionalHeaders: Map<String, String> = emptyMap()
): HttpURLConnection {
    val url = with(backend) { URL(route.composeCompleteUrl()) }
    val headers = defaultheaders.toMutableMap().apply {
        putAll(additionalHeaders)
    }
    val connection = NetworkBackendClient.makeRequest(
        url = url,
        method = method,
        body = body,
        options = RequestOptions(
            accessToken = accessCredentials?.accessToken,
            cookie = accessCredentials?.accessCookie,
            expectedResponseCodes = expectedResponseCodes
        ),
        headers = headers
    )

    WireTestLogger.getLog("TeamClient").info(connection.responseMessage)
    return connection
}

private fun BackendClient.updateSelfAssets(token: AccessToken?, assets: Set<Asset>) {
    NetworkBackendClient.sendJsonRequest(
        url = URL(TeamRoutes.SelfAssets.route.composeCompleteUrl()),
        method = "PUT",
        body = jsonOf(
            "assets" to JSONArray().apply {
                assets.forEach { put(it.toJson()) }
            }
        ).toString(),
        headers = defaultheaders,
        options = RequestOptions(accessToken = token),
    )
}

suspend fun BackendClient.updateUniqueUsername(user: ClientUser, newUniqueUsername: String) {
    var username = newUniqueUsername
    val tryAvoidDuplicates = username == user.uniqueUsername
    var ntry = 0
    while (true) {
        try {
            updateSelfHandle(getAuthToken(user), username)
            user.uniqueUsername = username
            return
        } catch (e: HttpRequestException) {
            if (tryAvoidDuplicates && e.returnCode == 409 && ntry < 5) {
                username = ClientUser.sanitizedRandomizedHandle(user.firstName)
            } else {
                throw e
            }
        }
        ntry++
    }
}

private fun BackendClient.updateSelfHandle(token: AccessToken?, handle: String) {
    NetworkBackendClient.sendJsonRequest(
        url = URL(TeamRoutes.SelfHandle.route.composeCompleteUrl()),
        method = "PUT",
        body = jsonOf("handle" to handle).toString(),
        headers = defaultheaders,
        options = RequestOptions(accessToken = token),
    )
}

fun BackendClient.getSelfDeletingMessagesSettings(teamMember: ClientUser): JSONObject {
    val teamId = Uri.encode(getTeamId(teamMember))
    val url = "i/teams/$teamId/features/selfDeletingMessages".composeCompleteUrl()

    val headers = defaultheaders.toMutableMap().apply {
        put(BackendClient.AUTHORIZATION, basicAuth.getEncoded())
    }

    val response = NetworkBackendClient.sendJsonRequestWithCookies(
        url = URL(url),
        method = "GET",
        headers = headers,
        options = RequestOptions()
    )

    return JSONObject(response.body)
}

suspend fun BackendClient.switchServiceForTeam(
    ownerOrAdminUser: ClientUser,
    teamId: String,
    providerId: String,
    serviceId: String,
    isEnabled: Boolean
) {
    val token = getAuthToken(ownerOrAdminUser)
    val url = URI("teams/$teamId/services/whitelist".composeCompleteUrl()).toURL()

    val headers = defaultheaders.toMutableMap().apply {
        put(BackendClient.AUTHORIZATION, "${token?.type} ${token?.value}")
        put(BackendClient.accept, "*/*")
    }

    val requestBody = JSONObject().apply {
        put("id", serviceId)
        put("provider", providerId)
        put("whitelisted", isEnabled)
    }

    NetworkBackendClient.sendJsonRequestWithCookies(
        url = url,
        method = "POST",
        headers = headers,
        body = requestBody.toString(),
        options = RequestOptions(
            accessToken = token,
            expectedResponseCodes = NumberSequence.Array(
                intArrayOf(HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_NO_CONTENT)
            )
        )
    )
}

suspend fun BackendClient.addServiceToConversation(asUser: ClientUser, serviceName: String, conversation: Conversation) {
    val teamId = conversation.teamId
        ?: throw IllegalStateException("Conversation '${conversation.name}' has no team id.")
    val service = getWhitelistedService(asUser, teamId, serviceName)
    val token = getAuthToken(asUser)
    val url = URI("conversations/${conversation.id}/bots".composeCompleteUrl()).toURL()

    val headers = defaultheaders.toMutableMap().apply {
        put(BackendClient.AUTHORIZATION, "${token?.type} ${token?.value}")
    }

    val requestBody = JSONObject().apply {
        put("service", service.getString("id"))
        put("provider", service.getString("provider"))
    }

    try {
        NetworkBackendClient.sendJsonRequestWithCookies(
            url = url,
            method = "POST",
            headers = headers,
            body = requestBody.toString(),
            options = RequestOptions(
                accessToken = token,
                expectedResponseCodes = NumberSequence.Array(
                    intArrayOf(HttpURLConnection.HTTP_CREATED, HttpURLConnection.HTTP_NO_CONTENT)
                )
            )
        )
    } catch (e: HttpRequestException) {
        throw HttpRequestException(
            "POST $url failed with HTTP ${e.returnCode}: ${e.message}",
            e.returnCode
        )
    }
}

private suspend fun BackendClient.getWhitelistedService(asUser: ClientUser, teamId: String, serviceName: String): JSONObject {
    val token = getAuthToken(asUser)
    val url = URI(
        "teams/$teamId/services/whitelisted?prefix=${Uri.encode(serviceName)}".composeCompleteUrl()
    ).toURL()

    val headers = defaultheaders.toMutableMap().apply {
        put(BackendClient.AUTHORIZATION, "${token?.type} ${token?.value}")
    }

    val response = NetworkBackendClient.sendJsonRequestWithCookies(
        url = url,
        method = "GET",
        headers = headers,
        options = RequestOptions(accessToken = token)
    )

    val services = JSONObject(response.body).getJSONArray("services")
    for (index in 0 until services.length()) {
        val service = services.getJSONObject(index)
        if (service.optString("name") == serviceName) {
            return service
        }
    }
    throw NoSuchElementException("Service '$serviceName' is not whitelisted for team '$teamId'.")
}

suspend fun BackendClient.enableMLSFeatureTeam(
    team: Team,
    defaultCipherSuite: Int,
    allowedCipherSuites: List<Int>,
    defaultProtocol: String,
    allowedProtocols: List<String>
) {
    val teamId = Uri.encode(team.id)
    val url = URI("i/teams/$teamId/features/mls".composeCompleteUrl()).toURL()

    val headers = defaultheaders.toMutableMap().apply {
        put("Authorization", basicAuth.getEncoded())
    }

    val requestBody = JSONObject().apply {
        put("status", "enabled")
        put(
            "config",
            JSONObject().apply {
                put("defaultCipherSuite", defaultCipherSuite)
                put("allowedCipherSuites", JSONArray(allowedCipherSuites))
                put("defaultProtocol", defaultProtocol)
                put("protocolToggleUsers", JSONArray())
                put("supportedProtocols", JSONArray(allowedProtocols))
            }
        )
    }

    NetworkBackendClient.sendJsonRequestWithCookies(
        url = url,
        method = "PUT",
        headers = headers,
        body = requestBody.toString(),
        options = RequestOptions(
            expectedResponseCodes = NumberSequence.Array(intArrayOf(HttpURLConnection.HTTP_OK))
        )
    )
}

suspend fun BackendClient.enableChannelFeatureViaBackdoorTeam(team: Team) {
    val teamId = Uri.encode(team.id)
    val headers = defaultheaders.toMutableMap().apply {
        put("Authorization", basicAuth.getEncoded())
    }

    NetworkBackendClient.sendJsonRequestWithCookies(
        url = URI("i/teams/$teamId/features/channels".composeCompleteUrl()).toURL(),
        method = "PATCH",
        headers = headers,
        body = JSONObject().put("status", "enabled").toString(),
        options = RequestOptions(
            expectedResponseCodes = NumberSequence.Array(intArrayOf(HttpURLConnection.HTTP_OK))
        )
    )
}

suspend fun BackendClient.enableCellsFeatureViaBackdoorTeam(team: Team) {
    val teamId = Uri.encode(team.id)
    val headers = defaultheaders.toMutableMap().apply {
        put("Authorization", basicAuth.getEncoded())
    }

    NetworkBackendClient.sendJsonRequestWithCookies(
        url = URI("i/teams/$teamId/features/cells".composeCompleteUrl()).toURL(),
        method = "PATCH",
        headers = headers,
        body = JSONObject().put("status", "enabled").toString(),
        options = RequestOptions(
            expectedResponseCodes = NumberSequence.Array(intArrayOf(HttpURLConnection.HTTP_OK))
        )
    )
}

suspend fun BackendClient.unlockChannelFeature(team: Team) {
    val teamId = Uri.encode(team.id)
    val url = URI("i/teams/$teamId/features/channels/unlocked".composeCompleteUrl()).toURL()

    val headers = defaultheaders.toMutableMap().apply {
        put("Authorization", basicAuth.getEncoded())
    }

    NetworkBackendClient.sendJsonRequestWithCookies(
        url = url,
        method = "PUT",
        headers = headers,
        body = JSONObject().toString(),
        options = RequestOptions(
            expectedResponseCodes = NumberSequence.Array(intArrayOf(HttpURLConnection.HTTP_OK))
        )
    )
}

suspend fun BackendClient.enableForceAppLockFeature(team: Team, seconds: Int) {
    val teamId = Uri.encode(team.id)
    val url = URI("i/teams/$teamId/features/appLock".composeCompleteUrl()).toURL()

    val headers = defaultheaders.toMutableMap().apply {
        put("Authorization", basicAuth.getEncoded())
    }

    val requestBody = JSONObject().apply {
        put("status", "enabled")
        put(
            "config",
            JSONObject().apply {
                put("enforceAppLock", true)
                put("inactivityTimeoutSecs", seconds)
            }
        )
    }

    NetworkBackendClient.sendJsonRequestWithCookies(
        url = url,
        method = "PUT",
        headers = headers,
        body = requestBody.toString(),
        options = RequestOptions(
            expectedResponseCodes = NumberSequence.Array(intArrayOf(HttpURLConnection.HTTP_OK))
        )
    )
}

fun ClientUser.deleteTeam(backend: BackendClient) {
    sendAuthenticatedRequest(
        backend = backend,
        route = "teams/$teamId",
        method = "DELETE",
        body = jsonOf("password" to password).toString()
    )
}

fun ClientUser.deleteTeamMember(
    backend: BackendClient,
    userIdOfMemberToDelete: String
) {
    sendAuthenticatedRequest(
        backend = backend,
        route = "teams/$teamId/members/$userIdOfMemberToDelete",
        method = "DELETE",
        body = jsonOf("password" to password).toString(),
        expectedResponseCodes = NumberSequence.Array(
            intArrayOf(
                HttpURLConnection.HTTP_OK,
                HttpURLConnection.HTTP_ACCEPTED
            )
        )
    )
}

fun ClientUser.suspendTeam(backend: BackendClient) {
    val encodedTeamId = URLEncoder.encode(teamId, "UTF-8")
    sendAuthenticatedRequest(
        backend = backend,
        route = "i/teams/$encodedTeamId/suspend",
        method = "POST",
        body = "",
        additionalHeaders = mapOf(BackendClient.contentType to BackendClient.applicationJson),
        expectedResponseCodes = NumberSequence.Array(intArrayOf(HttpURLConnection.HTTP_OK))
    )
}

@Suppress("TooGenericExceptionCaught", "MagicNumber")
private suspend fun <T> retryOnBackendFailure(action: () -> T): T {
    var ntry = 1
    var savedException: Exception? = null
    while (ntry <= 2) {
        try {
            return action()
        } catch (e: Exception) {
            savedException = e
            delay(2000L * ntry)
        }
        ntry++
    }
    throw savedException ?: Exception("Unknown Error")
}

enum class TeamRoutes(val route: String) {
    BookEmail("activate/send"),
    Register("register"),
    UploadAsset("assets/v3"),
    SelfAssets("self"),
    SelfHandle("self/handle"),
}

enum class TeamRoles(val role: String, val permissionBitMask: Int) {
    Owner("owner", 8191),
    Admin("admin", 5951),
    Member("member", 1587),
    Partner("partner", 1025),
    External("partner", 0),
    INVALID("invalid", 1234);

    companion object {
        fun getByPermissionBitMask(permissionBitMask: Int): TeamRoles =
            entries.firstOrNull {
                it.permissionBitMask == permissionBitMask
            }
                ?: throw NoSuchElementException(
                    "Permission bit mask '$permissionBitMask' is unknown"
                )
    }

    override fun toString(): String = role
}
