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
import androidx.test.platform.app.InstrumentationRegistry
import com.wire.android.testSupport.BuildConfig
import kotlinx.coroutines.delay
import network.BackendClient.Companion.sendJsonRequest
import org.json.JSONObject
import java.net.URL

// This object is responsible for interacting with the Inbucket email server used in testing
object InbucketClient {

    // This function fetches the 6-digit verification code from the latest email sent to the given address
    suspend fun getVerificationCode(email: String): String {

        // These values are pulled from BuildConfig (injected at build time from env.properties)
        val backendUrl = BuildConfig.BACKEND_URL
        val username = BuildConfig.INBUCKET_USERNAME
        val inbucketUrl = BuildConfig.INBUCKET_URL
        val password = BuildConfig.INBUCKET_PASSWORD


        val url = URL("https://$inbucketUrl/api/v1/mailbox/$email/latest")
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
}
