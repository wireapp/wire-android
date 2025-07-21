@file:Suppress("TooManyFunctions")
package com.wire.android.testSupport.backendConnections.team

import ImageUtil
import InbucketClient.getInbucketVerificationCode
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.wire.android.testSupport.R
import com.wire.android.testSupport.backendConnections.BackendClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import logger.WireTestLogger
import network.HttpRequestException
import network.HttpResponseWithCookies
import network.NetworkBackendClient
import network.NetworkBackendClient.accessCredentials
import network.NetworkBackendClient.response
import network.NumberSequence
import network.RequestOptions
import org.json.JSONArray
import org.json.JSONObject
import user.utils.AccessCookie
import user.utils.AccessCredentials
import user.utils.AccessToken
import user.utils.Asset
import user.utils.ClientUser
import java.io.IOException
import java.net.HttpURLConnection
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

private  fun BackendClient.sendTeamRequest(
    route: String,
    method: String,
    user: ClientUser? = null,
    body: String? = null,
    additionalHeaders: Map<String, String> = emptyMap()
): HttpResponseWithCookies {
    val headers = defaultheaders.toMutableMap().apply {
        putAll(additionalHeaders)
        user?.let {
            runBlocking {  getAuthToken(it)?.let { token ->
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

suspend fun BackendClient.createTeamOwnerViaBackend(
    user: ClientUser,
    teamName: String,
    locale: String,
    updateHandle: Boolean,
    context: Context,
): ClientUser {
    bookEmail(user.email.orEmpty())
    val activationCode = getActivationCodeForEmail(user.email.orEmpty())

    val response = sendTeamRequest(
        route = TeamRoutes.Register.route,
        method = "POST",
        body = jsonOf(
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
        ).toString()
    )

    user.updateFromResponse(response)
    updateUserPicture(user, context)

    if (updateHandle) {
        updateUniqueUsername(user, user.uniqueUsername.orEmpty())
    }
    return user
}

fun getImageInputStream(context: Context) = context.resources.openRawResource(R.drawable.default_team_avatar)

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
        val response = sendTeamRequest(
            route = TeamRoutes.Register.route,
            method = "POST",
            body = jsonOf(
                "email" to member.email,
                "name" to member.name,
                "password" to member.password,
                "team_code" to invitationCode
            ).toString()
        )

        member.updateFromResponse(response, teamId)

        if (uploadPicture) updateUserPicture(member, context)
        if (hasHandle) updateUniqueUsername(member, member.uniqueUsername.orEmpty())
    }
    return member
}

fun BackendClient.acceptInvite(teamId: String, member: ClientUser): ClientUser {
    val invitationCode = getTeamCode(teamId, member.id.orEmpty())
    val response = sendTeamRequest(
        route = TeamRoutes.Register.route,
        method = "POST",
        body = jsonOf(
            "email" to member.email,
            "name" to member.name,
            "password" to member.password,
            "team_code" to invitationCode
        ).toString()
    )
    member.updateFromResponse(response, teamId)
    return member
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

@Suppress("MagicNumber")
private suspend fun BackendClient.updateUserPicture(user: ClientUser, image: Bitmap) {
    val token = getAuthToken(user)
    val square = ImageUtil.cropToSquare(image)
    val preview = ImageUtil.scaleTo(square, 200, 200)
    val previewKey = retryOnBackendFailure {
        NetworkBackendClient.uploadAsset(
            URL(TeamRoutes.UploadAsset.route.composeCompleteUrl()),
            token,
            true,
            "eternal",
            ImageUtil.asByteArray(preview)
        )
    }
    val completeKey = retryOnBackendFailure {
        NetworkBackendClient.uploadAsset(
            URL(TeamRoutes.UploadAsset.route.composeCompleteUrl()),
            token,
            true,
            "eternal",
            ImageUtil.asByteArray(image)
        )
    }
    val assets = setOf(
        Asset(previewKey, "image", "preview"),
        Asset(completeKey, "image", "complete")
    )
    retryOnBackendFailure {
        updateSelfAssets(token, assets)
        null
    }
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

@Suppress("MagicNumber")
private suspend fun BackendClient.updateUniqueUsername(user: ClientUser, newUniqueUsername: String) {
    var username = newUniqueUsername
    val tryAvoidDuplicates = username.equals(user.uniqueUsername, true)
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

suspend fun BackendClient.getAuthToken(user: ClientUser): AccessToken? {
    return getAuthCredentials(user).accessToken
}

private suspend fun BackendClient.getAuthCredentials(user: ClientUser): AccessCredentials {
    val credentials = user.accessCredentials
    return when {
        credentials == null -> login(user).also {
            user.accessCredentials = it
        }
        credentials.accessToken == null || credentials.accessToken.isInvalid() || credentials.accessToken.isExpired() ->
            access(credentials).also {
                user.accessCredentials = it
            }
        else -> credentials
    }
}

@Suppress("MagicNumber")
private suspend fun BackendClient.login(user: ClientUser): AccessCredentials {
    val connection = NetworkBackendClient.makeRequest(
        url = URL("login".composeCompleteUrl()),
        method = "POST",
        body = jsonOf(
            "email" to user.email,
            "password" to user.password,
            "label" to ""
        ).toString(),
        options = RequestOptions(expectedResponseCodes = NumberSequence.Array(intArrayOf(200, 403))),
        headers = defaultheaders,
    )

    return when (connection.responseCode) {
        403 -> {
            if (inbucketUrl.isBlank()) {
                throw IOException("Received 403 for 2FA but no inbucket url present - check your backend settings")
            }
            val verificationCode = getInbucketVerificationCode(
                user.email ?: throw IllegalArgumentException("No email tied to user")
            )
            val connection2fa = NetworkBackendClient.makeRequest(
                url = URL("login".composeCompleteUrl()),
                method = "POST",
                body = jsonOf(
                    "email" to user.email,
                    "password" to user.password,
                    "verification_code" to verificationCode
                ).toString(),
                headers = defaultheaders,
            )
            connection2fa.accessCredentials(connection2fa.response())
        }
        else -> connection.accessCredentials(connection.response())
    }
}

fun ClientUser.deleteTeam(backend: BackendClient) {
    NetworkBackendClient.makeRequest(
        url = with(backend) { URL("teams/$teamId".composeCompleteUrl()) },
        method = "DELETE",
        body = jsonOf("password" to password).toString(),
        options = RequestOptions(
            accessToken = accessCredentials?.accessToken,
            cookie = accessCredentials?.accessCookie
        ),
        headers = defaultheaders,
    ).also { connection ->
        WireTestLogger.getLog("UserClient").info(connection.responseMessage)
    }
}

fun ClientUser.deleteTeamMember(
    backend: BackendClient,
    userIdOfMemberToDelete: String
) {
    NetworkBackendClient.makeRequest(
        url = with(backend) { URL("teams/$teamId/members/$userIdOfMemberToDelete".composeCompleteUrl()) },
        method = "DELETE",
        body = jsonOf("password" to password).toString(),
        options = RequestOptions(
            accessToken = accessCredentials?.accessToken,
            cookie = accessCredentials?.accessCookie
        ),
        headers = defaultheaders
    ).also { connection ->
        if (connection.responseCode !in listOf(HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_ACCEPTED)) {
            throw IOException("Delete team member failed with status ${connection.responseCode}")
        }
        WireTestLogger.getLog("TeamClient").info(connection.responseMessage)
    }
}

fun ClientUser.suspendTeam(backend: BackendClient) {
    val encodedTeamId = URLEncoder.encode(teamId, "UTF-8")
    NetworkBackendClient.makeRequest(
        url = with(backend) { URL("i/teams/$encodedTeamId/suspend".composeCompleteUrl()) },
        method = "POST",
        body = "",
        headers = defaultheaders.toMutableMap().apply {
            put("Content-Type", "application/json")
        },
        options = RequestOptions(
            accessToken = accessCredentials?.accessToken,
            cookie = accessCredentials?.accessCookie
        ),
    ).also { connection ->
        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw IOException("Suspend team failed with status ${connection.responseCode}")
        }
        WireTestLogger.getLog("TeamClient").info(connection.responseMessage)
    }
}

private fun BackendClient.access(credentials: AccessCredentials): AccessCredentials {
    val connection = NetworkBackendClient.makeRequest(
        url = URL("access".composeCompleteUrl()),
        method = "POST",
        body = jsonOf("withCredentials" to true).toString(),
        headers = defaultheaders,
        options = RequestOptions(
            accessToken = credentials.accessToken,
            cookie = credentials.accessCookie
        ),
    )
        return  connection.accessCredentials(connection.response())
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

val defaultheaders = mapOf(
    "Accept" to "application/json",
    "Content-Type" to "application/json"
)

enum class TeamRoles(val role: String) {
    Owner("Owner"),
    Admin("Admin"),
    Member("member"),
    Partner("Partner"),
    External("External")
}
