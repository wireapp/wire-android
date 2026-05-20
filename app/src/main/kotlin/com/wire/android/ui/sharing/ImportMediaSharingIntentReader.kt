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
package com.wire.android.ui.sharing

import android.content.Intent
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import com.wire.android.util.parcelableArrayList

fun AppCompatActivity.toImportMediaSharingContent(): ImportMediaSharingContent {
    val incomingIntent = ShareCompat.IntentReader(this)
    val assetUris = when {
        incomingIntent.streamCount == 0 -> emptyList()
        incomingIntent.isSingleShare -> listOfNotNull(incomingIntent.stream?.toString())
        else -> intent.parcelableArrayList<Parcelable>(Intent.EXTRA_STREAM)
            ?.map { it.toString() }
            .orEmpty()
    }
    return ImportMediaSharingContent(
        text = incomingIntent.text,
        assetUris = assetUris,
        streamCount = incomingIntent.streamCount,
        isSingleShare = incomingIntent.isSingleShare,
    )
}
