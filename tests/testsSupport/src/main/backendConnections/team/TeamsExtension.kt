package com.wire.android.testSupport.backendConnections.team

import AccessCredentials
import Backend
import android.content.Context
import network.BackendClient
import org.json.JSONObject
import user.utils.AccessCookie
import user.utils.ClientUser
import java.net.URL

private suspend fun Backend.bookEmail(email: String): String {
    val url = URL(TeamRoutes.BookEmail.route.composeCompleteUrl())

    val requestBody = JSONObject().apply {
        put("email", email)
    }

    val response = BackendClient.sendJsonRequest(
        url = url,
        method = "POST",
        body = requestBody.toString(),
        headers = defaultheaders
    )

    return "Sent activation email"
}

suspend fun Backend.createTeamOwnerViaBackdoor(
    user: ClientUser,
    teamName: String,
    locale: String,
    updateHandle: Boolean
): ClientUser {
    bookEmail(user.email.orEmpty())

    val activationCode = getActivationCodeForEmail(user.email.orEmpty())

    val url = URL(TeamRoutes.Register.route.composeCompleteUrl()) // Replace with actual endpoint
    val requestBody = JSONObject().apply {
        put("email", user.email)
        put("name", user.name)
        put("locale", locale)
        put("password", user.password)
        activationCode.let { put("email_code", it) }

        val team = JSONObject().apply {
            put("name", teamName)
            put("icon", "default")
            put("binding", true)
        }
        put("team", team)
    }
      val response = BackendClient.sendJsonRequestWithCookies(
        url = url,
        method = "POST",
        body = requestBody.toString(),
        headers = defaultheaders
    )


  val responseJson = JSONObject(response.body)

    user.id = responseJson.getString("id")
    user.teamId = responseJson.getString("team")



    val cookie = AccessCookie("zuid", response.cookies)
    user.accessCredentials = AccessCredentials(null, cookie)

    updateUserPicture(user)

    if (updateHandle) {
        updateUniqueUsername(user, user.uniqueUsername)
    }

    return user
}

fun getImageInputStream(context: Context) = context.getResources().openRawResource(R.drawable.default_team_avatar);



enum class TeamRoutes(val route: String) {
    BookEmail("activate/send"),
    Register("register")
}

val defaultheaders = mapOf(
    "Accept" to "application/json",
    "Content-Type" to "application/json"
)
