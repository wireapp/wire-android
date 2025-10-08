/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.util

import com.wire.android.appLogger
import java.net.IDN
import java.net.URI

fun containsSchema(url: String): Boolean {
    return try {
        URI.create(url).scheme != null
    } catch (iae: IllegalArgumentException) {
        false // invalid URI
    }
}

fun normalizeLink(url: String): String {
    val sanitizedUrl = sanitizeUrl(url)
    return if (containsSchema(sanitizedUrl)) {
        sanitizedUrl
    } else {
        "https://$sanitizedUrl"
    }
}

@Suppress("TooGenericExceptionCaught")
fun sanitizeUrl(url: String): String {
    try {
        val urlComponents = url.split("://", limit = 2)
        val scheme = urlComponents[0] // Extract the URL scheme (e.g., "http")
        val restOfUrl = urlComponents[1] // Extract the rest of the URL

        // Split the rest of the URL by '/' to isolate the domain
        val domainAndPath = restOfUrl.split("/")
        val domain = domainAndPath[0] // Extract the domain

        // Use IDN.toASCII to convert the domain to ASCII representation
        val asciiDomain = IDN.toASCII(domain)

        // Reconstruct the sanitized URL
        return "$scheme://$asciiDomain" +
                if (domainAndPath.size > 1) "/" + domainAndPath.subList(1, domainAndPath.size).joinToString("/") else ""
    } catch (e: Exception) {
        // Handle any exceptions that might occur during the processing
        appLogger.w("Error sanitizing URL: $url", e)
        return url // Return the original URL if any errors occur
    }
}

fun URI.removeQueryParams(): URI {
    val regex = Regex("[?&][^=]+=[^&]*")
    return URI(this.toString().replace(regex, ""))
}

@Suppress("TooGenericExceptionCaught")
fun URI.findParameterValue(parameterName: String): String? {
    return try {
        rawQuery.split('&').map {
            val parts = it.split('=')
            val name = parts.firstOrNull() ?: ""
            val value = parts.drop(1).firstOrNull() ?: ""
            Pair(name, value)
        }.firstOrNull { it.first == parameterName }?.second
    } catch (e: NullPointerException) {
        appLogger.w("Error finding parameter value: $parameterName", e)
        null
    }
}

fun String.isValidWebUrl() = (this.startsWith("http://") || this.startsWith("https://"))
        && android.util.Patterns.WEB_URL.matcher(this).matches()
