@file:Suppress("MagicNumber", "PackageNaming")
/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

import android.net.Uri
import backendUtils.BackendClient
import backendUtils.auth.defaultheaders
import backendUtils.user.setPropertyValue
import com.wire.android.testSupport.backendConnections.team.Team
import network.NetworkBackendClient
import network.NumberSequence
import network.RequestOptions
import org.json.JSONObject
import user.utils.ClientUser
import java.net.HttpURLConnection
import java.net.URI

suspend fun BackendClient.unlockConferenceCallingFeature(team: Team) {
    val teamId = Uri.encode(team.id)
    val url = URI("i/teams/$teamId/features/conferenceCalling/unlocked".composeCompleteUrl()).toURL()

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

suspend fun BackendClient.enableConferenceCallingBackdoorViaBackendTeam(team: Team) {
    val teamId = Uri.encode(team.id)
    val url = URI("i/teams/$teamId/features/conferenceCalling".composeCompleteUrl()).toURL()

    val headers = defaultheaders.toMutableMap().apply {
        put("Authorization", basicAuth.getEncoded())
    }

    val requestBody = JSONObject().apply {
        put("status", "enabled")
    }

    NetworkBackendClient.sendJsonRequestWithCookies(
        url = url,
        method = "PATCH",
        headers = headers,
        body = requestBody.toString(),
        options = RequestOptions(
            expectedResponseCodes = NumberSequence.Array(intArrayOf(HttpURLConnection.HTTP_OK))
        )
    )
}

suspend fun BackendClient.disableConferenceCallingBackdoorViaBackendTeam(team: Team) {
    val teamId = Uri.encode(team.id)
    val url = URI("i/teams/$teamId/features/conferenceCalling".composeCompleteUrl()).toURL()

    val headers = defaultheaders.toMutableMap().apply {
        put("Authorization", basicAuth.getEncoded())
    }

    val requestBody = JSONObject().apply {
        put("status", "disabled")
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

suspend fun BackendClient.enableConferenceCallingViaBackendPersonalUser(personalUser: ClientUser) {
    val userId = Uri.encode(personalUser.id)
    val url = URI("i/users/$userId/features/conferenceCalling".composeCompleteUrl()).toURL()

    val headers = defaultheaders.toMutableMap().apply {
        put("Authorization", basicAuth.getEncoded())
    }

    val requestBody = JSONObject().apply {
        put("status", "enabled")
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

suspend fun BackendClient.upgradeToEnterprisePlanResult(team: Team) {
    enableConferenceCallingBackdoorViaBackendTeam(team)
}

suspend fun BackendClient.getCallConfig(): JSONObject {
    val url = URI("calls/config/v2".composeCompleteUrl()).toURL()

    val headers = defaultheaders.toMutableMap().apply {
        put("Authorization", basicAuth.getEncoded())
    }

    val response = NetworkBackendClient.sendJsonRequestWithCookies(
        url = url,
        method = "GET",
        headers = headers,
        options = RequestOptions(
            expectedResponseCodes = NumberSequence.Array(intArrayOf(HttpURLConnection.HTTP_OK))
        )
    )

    return JSONObject(response.body)
}

suspend fun BackendClient.disableConsentPopup(user: ClientUser) {
    val privacyProperty = JSONObject().apply {
        put("improve_wire", false)
        put("marketing_consent", false)
        put("telemetry_data_sharing", false)
    }
    val settingsProperty = JSONObject().apply {
        put("privacy", privacyProperty)
    }
    val properties = JSONObject().apply {
        put("settings", settingsProperty)
    }
    setPropertyValue(user, "webapp", properties.toString())
}
