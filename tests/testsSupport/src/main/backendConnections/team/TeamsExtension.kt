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

private fun BackendClient.bookEmail(email: String): String {
    val url = URL(TeamRoutes.BookEmail.route.composeCompleteUrl())

    val requestBody = JSONObject().apply {
        put("email", email)
    }

    val response = NetworkBackendClient.sendJsonRequest(
        url = url,
        method = "POST",
        body = requestBody.toString(),
        headers = defaultheaders
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
    /**
     * Send a code to the team owner
     */
    bookEmail(user.email.orEmpty())

    val activationCode = getActivationCodeForEmail(user.email.orEmpty())

    val url = URL(TeamRoutes.Register.route.composeCompleteUrl()) // Replace with actual endpoint
    val requestBody = JSONObject().apply {
        put("email", user.email)
        put("name", user.name)
        put("locale", locale)
        put("password", user.password)
        put("email_code", activationCode)

        val team = JSONObject().apply {
            put("name", teamName)
            put("icon", "default")
            put("binding", true)
        }
        put("team", team)
    }
    val response = NetworkBackendClient.sendJsonRequestWithCookies(
        url = url,
        method = "POST",
        body = requestBody.toString(),
        headers = defaultheaders
    )

    val responseJson = JSONObject(response.body)

    println("\nResponse: ${response.cookies}")

    user.id = responseJson.getString("id")
    user.teamId = responseJson.getString("team")

    val cookie = AccessCookie("zuid", response.cookies)
    user.accessCredentials = AccessCredentials(null, cookie)

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
    val token = runBlocking {
        getAuthToken(teamOwner)
    }

    /**
     * send an invite email to the team member
     */
    val invitationId = inviteNewUserToTeam(
        token?.value.orEmpty(),
        teamId,
        member.email ?: "",
        teamOwner.name ?: "",
        role
    )

    /**
     * get the invite code from the mail we sent
     */
    val invitationCode = getTeamCode(teamId, invitationId)

    val url = URL(TeamRoutes.Register.route.composeCompleteUrl())
    val requestBody = JSONObject().apply {
        put("email", member.email)
        put("name", member.name)
        put("password", member.password)
        put("team_code", invitationCode)
    }

    val headers = defaultheaders

    val response = NetworkBackendClient.sendJsonRequestWithCookies(
        url = url,
        method = "POST",
        body = requestBody.toString(),
        headers = headers
    )

    val jsonResponse = JSONObject(response.body)

    member.id = jsonResponse.getString("id")
    member.teamId = teamId

    val accessCookie = AccessCookie("zuid", response.cookies)
    member.accessCredentials = AccessCredentials(null, accessCookie)

    if (uploadPicture) {
        runBlocking {
            updateUserPicture(member, context)
        }
    }

    if (hasHandle) {
        runBlocking {
            updateUniqueUsername(member, member?.uniqueUsername.orEmpty())
        }
    }

    return member
}

fun BackendClient.acceptInvite(teamId: String, member: ClientUser): ClientUser {
    val invitationCode = getTeamCode(teamId, member.id.orEmpty())

    val url = URL(TeamRoutes.Register.route.composeCompleteUrl())
    val requestBody = JSONObject().apply {
        put("email", member.email)
        put("name", member.name)
        put("password", member.password)
        put("team_code", invitationCode)
    }

    val headers = defaultheaders

    val response = NetworkBackendClient.sendJsonRequestWithCookies(
        url = url,
        method = "POST",
        body = requestBody.toString(),
        headers = headers
    )

    val jsonResponse = JSONObject(response.body)

    member.id = jsonResponse.getString("id")
    member.teamId = teamId

    val accessCookie = AccessCookie("zuid", response.cookies)
    member.accessCredentials = AccessCredentials(null, accessCookie)

    return member
}

fun BackendClient.getTeamCode(teamId: String, invitationId: String): String {
    val encodedTeamId = URLEncoder.encode(teamId, "UTF-8")
    val encodedInvitationId = URLEncoder.encode(invitationId, "UTF-8")
    val url = URL("i/teams/invitation-code?team=$encodedTeamId&invitation_id=$encodedInvitationId".composeCompleteUrl())

    val headers = defaultheaders.toMutableMap().apply {
        put("Authorization", basicAuth.getEncoded())
    }

    val response = NetworkBackendClient.sendJsonRequest(
        url = url,
        method = "GET",
        body = null,
        headers = headers
    )

    return JSONObject(response).getString("code")
}

fun BackendClient.inviteNewUserToTeam(
    token: String,
    teamId: String,
    dstEmail: String,
    inviterName: String,
    role: TeamRoles
): String {

    val url = URL("teams/$teamId/invitations".composeCompleteUrl())

    val requestBody = JSONObject().apply {
        put("email", dstEmail)
        put("role", role.role)
        put("inviter_name", inviterName)
    }

    val headers = defaultheaders.toMutableMap().apply {
        put("Authorization", "Bearer $token")
    }

    val response = NetworkBackendClient.sendJsonRequest(
        url = url,
        method = "POST",
        body = requestBody.toString(),
        headers = headers
    )

    val objectResponse = JSONObject(response)
    return objectResponse.getString("id")
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
        body = JSONObject().apply {
            val array = JSONArray()
            assets.forEach {
                array.put(it.toJson())
            }
            put("assets", array)
        }.toString(),
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
                // Try to generate another handle if this one already exists
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
        body = JSONObject().apply {
            put("handle", handle)
        }.toString(),
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
        body = JSONObject().apply {
            put("email", user.email)
            put("password", user.password)
            put("label", "")
        }.toString(),
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
                body = JSONObject().apply {
                    put("email", user.email)
                    put("password", user.password)
                    put("verification_code", verificationCode)
                }.toString(),
                headers = defaultheaders,
            )
            connection2fa.accessCredentials(connection2fa.response())
        }

        else -> {
            connection.accessCredentials(connection.response())
        }
    }
}

fun ClientUser.deleteTeam(backend: BackendClient) {
    val connection = NetworkBackendClient.makeRequest(
        url = with(backend) { URL("teams/$teamId".composeCompleteUrl()) },
        method = "DELETE",
        body = JSONObject().apply {
            put("password", password)
        }.toString(),
        options = RequestOptions(
            accessToken = accessCredentials?.accessToken,
            cookie = accessCredentials?.accessCookie
        ),
        headers = defaultheaders,
    )
    WireTestLogger.getLog("UserClient").info(connection.responseMessage)
}

fun ClientUser.deleteTeamMember(
    backend: BackendClient,
    userIdOfMemberToDelete: String
) {
    val connection = NetworkBackendClient.makeRequest(
        url = with(backend) { URL("teams/$teamId/members/$userIdOfMemberToDelete".composeCompleteUrl()) },
        method = "DELETE",
        body = JSONObject().apply {
            put("password", password)
        }.toString(),
        options = RequestOptions(
            accessToken = accessCredentials?.accessToken,
            cookie = accessCredentials?.accessCookie
        ),
        headers = defaultheaders
    )

    if (connection.responseCode !in listOf(HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_ACCEPTED)) {
        throw IOException("Delete team member failed with status ${connection.responseCode}")
    }

    WireTestLogger.getLog("TeamClient").info(connection.responseMessage)
}

fun ClientUser.suspendTeam(backend: BackendClient) {
    val encodedTeamId = URLEncoder.encode(teamId, "UTF-8")
    val url = with(backend) {
        URL("i/teams/$encodedTeamId/suspend".composeCompleteUrl())
    }

    NetworkBackendClient.makeRequest(
        url = url,
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
        body = JSONObject().apply {
            put("withCredentials", true)
        }.toString(),
        headers = defaultheaders,
        options = RequestOptions(
            accessToken = credentials.accessToken,
            cookie = credentials.accessCookie
        ),

        )
    return connection.accessCredentials(connection.response())
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
