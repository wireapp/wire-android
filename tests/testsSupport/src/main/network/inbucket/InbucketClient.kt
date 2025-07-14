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
import android.util.Base64
import com.wire.android.testSupport.backendConnections.BackendClient
import kotlinx.coroutines.delay
import logger.WireTestLogger
import network.NetworkBackendClient.sendJsonRequest
import org.json.JSONObject
import java.io.IOException
import java.net.URL

// This object is responsible for interacting with the Inbucket email server used in testing
object InbucketClient {

    @Suppress("TooGenericExceptionCaught", "MagicNumber")
    // This function fetches the 6-digit verification code from the latest email sent to the given address
    suspend fun getVerificationCode(email: String, inbucketUrl: String, password: String, username: String): String {

        val url = URL("$inbucketUrl/api/v1/mailbox/$email/latest")
        WireTestLogger.getLog("Inbucket").info("Url is :  " + url.toString())
        val loginString = "$username:$password"
        val base64Login = Base64.encodeToString(loginString.toByteArray(), Base64.NO_WRAP)

        val headers = mapOf(
            "Authorization" to "Basic $base64Login"
        )

        // Declare variables for response and retry counter
        var response: String
        var timeout = 0

        // Retry loop: keeps trying to fetch the email until it succeeds or times out
        while (true) {
            try {
                response = sendJsonRequest(
                    url = url,
                    method = "GET",
                    headers = headers
                )
                break
            } catch (e: Exception) {
                if (++timeout >= 100) throw e
                delay(300)
            }
        }

        val json = JSONObject(response)
        val subject = json.getString("subject")
        val verificationCode = subject.take(6)

        println("Verification Code Found: $verificationCode for $email")
        return verificationCode
    }

    @Suppress("TooGenericExceptionCaught", "MagicNumber")
    suspend fun getVerificationLink(
        email: String,
        inbucketUrl: String,
        password: String,
        username: String
    ): String {
        val url = URL("$inbucketUrl/api/v1/mailbox/$email/latest")
        WireTestLogger.getLog("Inbucket").info("Url is :  $url")
        val loginString = "$username:$password"
        val base64Login = Base64.encodeToString(loginString.toByteArray(), Base64.NO_WRAP)

        val headers = mapOf(
            "Authorization" to "Basic $base64Login"
        )

        var response: String
        var timeout = 0

        while (true) {
            try {
                response = sendJsonRequest(
                    url = url,
                    method = "GET",
                    headers = headers
                )
                break
            } catch (e: Exception) {
                if (++timeout >= 100) throw e
                delay(300)
            }
        }

        val json = JSONObject(response)
        val body = json.getJSONObject("body").getString("text")

        // Match any https URL containing key= and code=
        val regex = Regex("""https://[^\s\]]*key=[^&\s]+&code=\d+""")
        val match = regex.find(body)
            ?: throw IllegalStateException("Verification link not found in email body")

        val verificationLink = match.value
        println("Verification Link Found: $verificationLink for $email")
        return verificationLink
    }

    suspend fun BackendClient.getInbucketVerificationCode(email: String): String {
        if (inbucketUrl.isBlank()) {
            throw IOException("Received 403 for 2FA but no inbucket url present - check your backend settings")
        }
        trigger2FA(email)
        return getVerificationCode(email, inbucketUrl, inbucketPassword, inbucketUsername)
    }
}
