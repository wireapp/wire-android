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
package com.wire.android.tests.core.services

import com.wire.android.tests.core.utils.ZetaLogger
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.logging.Logger

class MixPanelMockAPIClient : MixPanelAPIClient {

    companion object {
        private val log: Logger = ZetaLogger.getLog(MixPanelAPIClient::class.simpleName)
        private const val MIXPANEL_LAST_REQUEST_FOR_DISTINCT_URL =
            "http://mixpanel.com/api/distinct/%s/event/%s/last/1"
    }


    @Throws(IOException::class)
    override fun getTrackingPropertiesFromLastEvent(distinctId: String?, event: String?): JSONObject {
        val client = OkHttpClient()
        val url = MIXPANEL_LAST_REQUEST_FOR_DISTINCT_URL.format(distinctId, event)
        log.info("Looking for action '$event' by user with distinct_id '$distinctId' at $url")
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            val contentString = response.body?.string() ?: throw IOException("Empty response body")
            val jsonArray = JSONArray(contentString)
            return if (jsonArray.length() == 0) {
                JSONObject()
            } else {
                jsonArray.getJSONObject(0).getJSONObject("properties")
            }
        }
    }
}

