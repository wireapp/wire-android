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

import androidx.test.platform.app.InstrumentationRegistry
import com.wire.android.testSupport.BuildConfig
import network.BackendClient
import org.json.JSONObject
import java.net.URL

class LoginClient {
    companion object {


        @Throws(Exception::class)
        fun loginViaAPI(email: String, password: String): String {
            val backendUrl = BuildConfig.BACKENDCONNECTION_STAGING_BACKENDURL

            print(backendUrl ?: "")

            val url = URL("$backendUrl/v8/login")
            val body = JSONObject()
                .put("email", email)
                .put("password", password)
                .toString()

            val response = BackendClient.sendJsonRequest(url, "POST", body, headers = emptyMap())
            val json = JSONObject(response)
            return json.getString("access_token")
        }

        @Throws(Exception::class)
        fun deletePersonalUser(accessToken: String, password: String) {
            val args = InstrumentationRegistry.getArguments()
            val backendUrl = args.getString("BACKEND_URL")
            val url = URL("$backendUrl/self")
            val body = JSONObject()
                .put("password", password)
                .toString()

            BackendClient.sendJsonRequest(
                url, "DELETE", body, headers = mapOf(
                    "Authorization" to "Bearer $accessToken"
                )
            )
        }
    }
}
