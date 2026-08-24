/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.authentication

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets.UTF_8

@Suppress("ReturnCount")
fun String.toBackendConfigUrl(): String? {
    val sanitizedInput = trim().takeIf(String::isNotBlank) ?: return null
    if (!sanitizedInput.startsWith(WIRE_ACCESS_DEEPLINK_BASE)) return sanitizedInput
    return runCatching {
        URI.create(sanitizedInput)
            .rawQuery
            ?.split('&')
            ?.firstOrNull { it.substringBefore('=') == BACKEND_CONFIG_QUERY_PARAMETER }
            ?.substringAfter('=', missingDelimiterValue = "")
            ?.let { URLDecoder.decode(it, UTF_8.name()) }
            ?.takeIf(String::isNotBlank)
    }.getOrNull()
}

private const val WIRE_ACCESS_DEEPLINK_BASE = "wire://access/"
private const val BACKEND_CONFIG_QUERY_PARAMETER = "config"
