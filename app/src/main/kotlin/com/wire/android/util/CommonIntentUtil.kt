/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.wire.android.appLogger

// geo intent url scheme
internal const val GEO_INTENT_URL = "geo:0,0?q=%f,%f"

/**
 * Launches a geo intent with the given latitude and longitude.
 * If no app/activity can be found to handle the [GEO_INTENT_URL], a [fallbackUrl] is used.
 */
fun launchGeoIntent(
    latitude: Float,
    longitude: Float,
    placeName: String?,
    fallbackUrl: String,
    context: Context
) {
    val geoStringUrl = StringBuilder(String.format(GEO_INTENT_URL, latitude, longitude))
    if (!placeName.isNullOrEmpty()) geoStringUrl.append("(${Uri.encode(placeName)})")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, geoStringUrl.toString().toUri()))
    } catch (e: ActivityNotFoundException) {
        appLogger.e("No activity found to handle geo intent, fallback to url", e)
        context.startActivity(Intent(Intent.ACTION_VIEW, fallbackUrl.toUri()))
    }
}
