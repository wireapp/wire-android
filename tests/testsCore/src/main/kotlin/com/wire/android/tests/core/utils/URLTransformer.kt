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
package com.wire.android.tests.core.utils

import java.net.URI
import java.net.URISyntaxException

object URLTransformer {

    fun addQueryParameter(url: String, parameter: String): String {
        val uri = url.toUri()
        val newQuery = uri.query?.let { "$it&$parameter" } ?: parameter
        return URI(
            uri.scheme,
            uri.authority,
            uri.path,
            newQuery,
            uri.fragment
        ).toString()
    }

    fun removeQueryParameter(url: String, parameter: String): String {
        val uri = url.toUri()
        val query = uri.query ?: return url
        val newQuery = when {
            "&" in query -> query.split("&")
                .filterNot { it.startsWith("$parameter=") }
                .joinToString("&")
            else -> if (query.startsWith("$parameter=")) "" else query
        }
        return URI(
            uri.scheme,
            uri.authority,
            uri.path,
            newQuery.ifEmpty { null },
            uri.fragment
        ).toString()
    }

    fun getQueryParameter(url: String, parameterName: String): String? {
        val query = url.toUri().query ?: return null
        return query.split("&")
            .firstOrNull { it.startsWith("$parameterName=") }
            ?.substringAfter("=")
    }

    fun changePath(url: String, path: String): String {
        val uri = url.toUri()
        return URI(
            uri.scheme,
            uri.authority,
            path,
            uri.query,
            uri.fragment
        ).toString()
    }

    fun changePathAndQuery(url: String, pathAndQuery: String): String {
        var newUrl = url
        var path = pathAndQuery

        if ("?" in pathAndQuery) {
            val parameter = pathAndQuery.substringAfter("?")
            if ("#" in parameter) {
                val fragment = parameter.substringAfter("#")
                newUrl = changeFragment(newUrl, fragment)
                path = pathAndQuery.substringBefore("#")
            }
            newUrl = addQueryParameter(newUrl, pathAndQuery.substringAfter("?"))
            path = pathAndQuery.substringBefore("?")
        }
        return changePath(newUrl, path)
    }

    fun changeFragment(url: String, fragment: String): String {
        val uri = url.toUri()
        return URI(
            uri.scheme,
            uri.authority,
            uri.path,
            uri.query,
            fragment
        ).toString()
    }

    fun changeHost(url: String, host: String): String {
        val uri = url.toUri()
        return URI(
            uri.scheme,
            host,
            uri.path,
            uri.query,
            uri.fragment
        ).toString()
    }

    fun getHost(url: String): String = url.toUri().host

    fun getPath(url: String): String = url.toUri().path

    fun getQuery(url: String): String? = url.toUri().query

    private fun String.toUri(): URI = try {
        URI(this)
    } catch (e: URISyntaxException) {
        throw RuntimeException("Input URL is wrong: $this", e)
    }
}
